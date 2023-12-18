package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MacOSPowermetricsSensor implements PowerSensor {
    public static final String CPU = "CPU";
    public static final String GPU = "GPU";
    public static final String ANE = "ANE";
    public static final String DRAM = "DRAM";
    public static final String DCS = "DCS";
    public static final String PACKAGE = "Package";
    private static final String COMBINED = "Combined";
    public static final String CPU_SHARE = "cpuShare";
    private static final String POWER_INDICATOR = " Power: ";
    private static final int POWER_INDICATOR_LENGTH = POWER_INDICATOR.length();
    private Process powermetrics;
    private static final SensorMetadata.ComponentMetadata cpu = new SensorMetadata.ComponentMetadata(CPU, 0, "CPU power", true, "mW");
    private static final SensorMetadata.ComponentMetadata gpu = new SensorMetadata.ComponentMetadata(GPU, 1, "GPU power", true, "mW");
    private static final SensorMetadata.ComponentMetadata ane = new SensorMetadata.ComponentMetadata(ANE, 2, "Apple Neural Engine power", false, "mW");
    private static final SensorMetadata.ComponentMetadata cpuShare = new SensorMetadata.ComponentMetadata(CPU_SHARE, 3, "Computed share of CPU", false, "decimal percentage");
    private final Map<String, RegisteredPID> trackedPIDs = new ConcurrentHashMap<>();

    private final SensorMetadata metadata;

    public MacOSPowermetricsSensor() {
        // extract metadata
        try {
            final var exec = new ProcessBuilder().command("sudo", "powermetrics", "--samplers cpu_power", "-i 10", "-n 1").start();
            exec.waitFor(20, TimeUnit.MILLISECONDS);
            this.metadata = initMetadata(exec.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    MacOSPowermetricsSensor(InputStream inputStream) {
        this.metadata = initMetadata(inputStream);
    }

    SensorMetadata initMetadata(InputStream inputStream) {
        // init map with known components
        Map<String, SensorMetadata.ComponentMetadata> components = new HashMap<>();
        components.put(CPU, cpu);
        components.put(GPU, gpu);
        components.put(ANE, ane);
        components.put(CPU_SHARE, cpuShare);

        int headerLinesToSkip = 10;
        try (BufferedReader input = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = input.readLine()) != null) {
                if (headerLinesToSkip != 0) {
                    headerLinesToSkip--;
                    continue;
                }

                // skip empty / header lines
                if (line.isEmpty() || line.startsWith("*")) {
                    continue;
                }

                // looking for line fitting the: "<name> Power: xxx mW" pattern, where "name" will be a considered metadata component
                final var powerIndex = line.indexOf(" Power");
                // lines with `-` as the second char are disregarded as of the form: "E-Cluster Power: 6 mW" which fits the metadata pattern but shouldn't be considered
                if (powerIndex >= 0 && '-' != line.charAt(1)) {
                    addComponentTo(line.substring(0, powerIndex), components);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new SensorMetadata(components, "macOS powermetrics derived information, see https://firefox-source-docs.mozilla.org/performance/powermetrics.html");
    }


    private static void addComponentTo(String name, Map<String, SensorMetadata.ComponentMetadata> components) {
        switch (name) {
            case CPU, GPU, ANE:
                // already pre-added
                break;
            case COMBINED:
                // should be ignored
                break;
            default:
                components.put(name, new SensorMetadata.ComponentMetadata(name, components.size(), name, false, "mW"));
        }
    }

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
            int headerLinesToSkip = 10;
            // copy the pids so that we can remove them as soon as we've processed them
            final var pidsToProcess = new HashSet<>(trackedPIDs.keySet());
            // start measure
            final var measures = new HashMap<RegisteredPID, ProcessRecord>(trackedPIDs.size());
            final var powerComponents = new HashMap<String, Integer>(metadata.componentCardinality());
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

                extractPowerComponents(line, powerComponents);

                // we need an exit condition to break out of the loop, otherwise we'll just keep looping forever since there are always new lines since the process is periodical
                // so break out once we've found all the extracted components (in this case, only cpuShare is not extracted)
                // fixme: perhaps we should relaunch the process on each update loop instead of keeping it running? Not sure which is more efficient
                if(powerComponents.size() == metadata.componentCardinality() - 1) {
                    break;
                }
            }

            final var hasGPU = totalSampledGPU != 0;
            double finalTotalSampledGPU = totalSampledGPU;
            double finalTotalSampledCPU = totalSampledCPU;
            final var results = new HashMap<RegisteredPID, double[]>(measures.size());
            measures.forEach((pid, record) -> {
                final var cpuShare = record.cpu / finalTotalSampledCPU;
                final var measure = new double[metadata.componentCardinality()];

                metadata.components().forEach((name, cm) -> {
                    final var index = cm.index();
                    final var value = CPU_SHARE.equals(name) ? cpuShare : powerComponents.getOrDefault(name, 0);

                    if (cm.isAttributed()) {
                        final double attributionFactor;
                        if (GPU.equals(name)) {
                            attributionFactor = hasGPU ? record.gpu / finalTotalSampledGPU : 0.0;
                        } else {
                            attributionFactor = cpuShare;
                        }
                        measure[index] = value * attributionFactor;
                    } else {
                        measure[index] = value;
                    }

                    results.put(pid, measure);
                });
            });
            return results;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void extractPowerComponents(String line, HashMap<String, Integer> powerComponents) {
        // looking for line fitting the: "<name> Power: xxx mW" pattern and add all of the associated values together
        final var powerIndex = line.indexOf(POWER_INDICATOR);
        // lines with `-` as the second char are disregarded as of the form: "E-Cluster Power: 6 mW" which fits the pattern but shouldn't be considered
        // also ignore Combined Power if available since it is the sum of the other components
        if (powerIndex >= 0 && '-' != line.charAt(1) && !line.startsWith("Combined")) {
            // get component name
            final var name = line.substring(0, powerIndex);
            // extract power value
            final int value;
            try {
                value = Integer.parseInt(line.substring(powerIndex + POWER_INDICATOR_LENGTH, line.indexOf('m') - 1));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot parse power value from line '" + line + "'", e);
            }
            powerComponents.put(name, value);
        }
    }

    public void start(long frequency) throws Exception {
        if (!isStarted()) {
            // it takes some time for the external process in addition to the sampling time so adjust the sampling frequency to account for this so that at most one measure occurs during the sampling time window
            final var freq = Long.toString(frequency - 50);
//            powermetrics = new ProcessBuilder().command("sudo", "powermetrics", "--samplers cpu_power,tasks", "--show-process-samp-norm", "--show-process-gpu", "-i " + freq).start(); // for some reason this doesn't work
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
        System.out.println("tick = " + tick);
        System.out.println("trackedPIDs = " + trackedPIDs);
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
