package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MacOSPowermetricsSensor implements PowerSensor {
    public static final String CPU = "cpu";
    public static final String GPU = "gpu";
    public static final String ANE = "ane";
    public static final String CPU_SHARE = "cpuShare";
    private Process powermetrics;
    private final SensorMetadata.ComponentMetadata cpu = new SensorMetadata.ComponentMetadata(CPU, 0, "CPU power", true, "mW");
    private final SensorMetadata.ComponentMetadata gpu = new SensorMetadata.ComponentMetadata(GPU, 1, "GPU power", true, "mW");
    private final SensorMetadata.ComponentMetadata ane = new SensorMetadata.ComponentMetadata(ANE, 2, "Apple Neural Engine power", false, "mW");
    private final SensorMetadata.ComponentMetadata cpuShare = new SensorMetadata.ComponentMetadata(CPU_SHARE, 3, "Computed share of CPU", false, "decimal percentage");
    private final Map<String, RegisteredPID> trackedPIDs = new HashMap<>();

    private final SensorMetadata metadata = new SensorMetadata() {
        @Override
        public ComponentMetadata metadataFor(String component) {
            return switch (component) {
                case CPU -> cpu;
                case GPU -> gpu;
                case ANE -> ane;
                case CPU_SHARE -> cpuShare;
                default -> throw new IllegalArgumentException("Unknown component: '" + component + "'");
            };
        }

        @Override
        public int componentCardinality() {
            return 4;
        }
    };

    @Override
    public SensorMetadata metadata() {
        return metadata;
    }

    private static class ProcessRecord {
        final double cpu;
        final double gpu;
        final String pid;

        public ProcessRecord(String line) throws IllegalArgumentException {
            //Name                               ID     CPU ms/s  samp ms/s  User%  Deadlines (<2 ms, 2-5 ms)  Wakeups (Intr, Pkg idle)  GPU ms/s
            //iTerm2                             1008   46.66     46.91      83.94  0.00    0.00               30.46   0.00              0.00
            final var processData = line.split("\\s+");
            if (processData.length != 10) {
                throw new IllegalArgumentException("Received line doesn't conform to expected format: " + line);
            }
            pid = " " + processData[1] + " "; // pad to match prepared version for matching
            cpu = Double.parseDouble(processData[3]);
            gpu = Double.parseDouble(processData[9]);
        }
    }

    @Override
    public RegisteredPID register(long pid) {
        final var key = RegisteredPID.prepare(pid);
        final var registeredPID = new RegisteredPID(key);
        trackedPIDs.put(key, registeredPID);
        return registeredPID;
    }

    Map<RegisteredPID, double[]> extractPowerMeasure(InputStream powerMeasureInput) {
        try {
            // Should not be closed since it closes the process
            BufferedReader input = new BufferedReader(new InputStreamReader(powerMeasureInput));
            String line;

            double totalSampledCPU = -1;
            double totalSampledGPU = -1;
            double totalCPUPower = 0;
            double totalGPUPower = 0;
            double totalANEPower = 0;
            int headerLinesToSkip = 6;
            // copy the pids so that we can remove them as soon as we've processed them
            final var pidsToProcess = new HashSet<>(trackedPIDs.keySet());
            // start measure
            final var measures = new HashMap<RegisteredPID, ProcessRecord>(trackedPIDs.size());
            while ((line = input.readLine()) != null) {
                if (headerLinesToSkip != 0) {
                    headerLinesToSkip--;
                    continue;
                }

                if (line.isEmpty() || line.startsWith("*")) {
                    continue;
                }

                // first, look for process line detailing share
                if (!pidsToProcess.isEmpty()) {
                    if (pidsToProcess.stream().anyMatch(line::contains)) {
                        final var procInfo = new ProcessRecord(line);
                        measures.put(trackedPIDs.get(procInfo.pid), procInfo);
                        pidsToProcess.remove(procInfo.pid);
                    }
                    continue;
                }

                if (totalSampledCPU < 0) {
                    // then skip all lines until we get the totals
                    if (line.startsWith("ALL_TASKS")) {
                        final var totals = new ProcessRecord(line);
                        // compute ratio
                        totalSampledCPU = totals.cpu;
                        totalSampledGPU = totals.gpu > 0 ? totals.gpu : 0;
                    }
                    continue;
                }

                if (totalCPUPower == 0) {
                    // look for line that contains CPU power measure
                    if (line.startsWith("CPU Power")) {
                        totalCPUPower = extractMeasure(line);
                    }
                    continue;
                }

                if (line.startsWith("GPU Power")) {
                    totalGPUPower = extractMeasure(line);
                    continue;
                }

                if (line.startsWith("ANE Power")) {
                    totalANEPower = extractMeasure(line);
                    break;
                }
            }

            final var noGPU = totalSampledGPU == 0;
            double finalTotalSampledGPU = totalSampledGPU;
            double finalTotalGPUPower = totalGPUPower;
            double finalTotalSampledCPU = totalSampledCPU;
            double finalTotalCPUPower = totalCPUPower;
            double finalTotalANEPower = totalANEPower;
            final var results = new HashMap<RegisteredPID, double[]>(measures.size());
            measures.forEach((pid, record) -> {
                final var attributedGPU = noGPU ? 0.0 : record.gpu / finalTotalSampledGPU * finalTotalGPUPower;
                final var cpuShare = record.cpu / finalTotalSampledCPU;
                results.put(pid, new double[]{
                        cpuShare * finalTotalCPUPower,
                        attributedGPU,
                        finalTotalANEPower,
                        cpuShare});
            });
            return results;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static double extractMeasure(String line) {
        final var powerValue = line.split(":")[1];
        final var powerInMilliwatts = powerValue.split("m")[0];
        return Double.parseDouble(powerInMilliwatts);
    }

    public void start(long frequency) throws Exception {
        if (!isStarted()) {
            // it takes some time for the external process in addition to the sampling time so adjust the sampling frequency to account for this so that at most one measure occurs during the sampling time window
            final var freq = Long.toString(frequency - 50);
            powermetrics = Runtime.getRuntime()
                    .exec("sudo powermetrics --samplers cpu_power,tasks --show-process-samp-norm --show-process-gpu -i " + freq);
        }
    }

    @Override
    public boolean isStarted() {
        return powermetrics != null && powermetrics.isAlive();
    }

    @Override
    public Map<RegisteredPID, double[]> update(Long tick) {
        return extractPowerMeasure(powermetrics.getInputStream());
    }

    @Override
    public void stop() {
        powermetrics.destroy();
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        trackedPIDs.remove(registeredPID.stringForMatching());
        // if we're not tracking any processes anymore, stop powermetrics as well
        if (trackedPIDs.isEmpty()) {
            stop();
        }
    }
}
