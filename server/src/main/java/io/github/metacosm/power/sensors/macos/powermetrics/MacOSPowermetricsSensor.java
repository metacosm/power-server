package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMeasure;
import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MacOSPowermetricsSensor implements PowerSensor {
    public static final String CPU = "CPU";
    public static final String GPU = "GPU";
    public static final String ANE = "ANE";
    public static final String DRAM = "DRAM";
    public static final String DCS = "DCS";
    public static final String PACKAGE = "Package";
    public static final String CPU_SHARE = "cpuShare";
    private static final String POWER_INDICATOR = " Power: ";
    private static final int POWER_INDICATOR_LENGTH = POWER_INDICATOR.length();
    private Process powermetrics;
    private final Measures measures = new MapMeasures();
    private final CPU cpu;

    public MacOSPowermetricsSensor() {
        // extract metadata
        try {
            final var exec = new ProcessBuilder().command("sudo", "powermetrics", "--samplers cpu_power", "-i 10", "-n 1").start();
            if(exec.waitFor(20, TimeUnit.MILLISECONDS)) {
                this.cpu = initMetadata(exec.getInputStream());
            } else {
                throw new IllegalStateException("Couldn't execute powermetrics to extract metadata");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    MacOSPowermetricsSensor(InputStream inputStream) {
        this.cpu = initMetadata(inputStream);
    }

    CPU initMetadata(InputStream inputStream) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            CPU cpu = null;
            Map<String, SensorMetadata.ComponentMetadata> components = new HashMap<>();
            while ((line = input.readLine()) != null) {
                if (cpu == null) {
                    // if we reached the OS line while cpu is still null, we're looking at an Apple Silicon CPU
                    if (line.startsWith("OS ")) {
                        cpu = new AppleSiliconCPU();
                        cpu.initComponents(components);
                        continue;
                    }

                    if (line.startsWith("EFI ")) {
                        cpu = new IntelCPU();
                        cpu.initComponents(components);
                    }
                } else {
                    // skip empty / header lines
                    if (line.isEmpty() || line.startsWith("*")) {
                        continue;
                    }

                    cpu.addComponentIfFound(line, components);
                }
            }

            if (cpu == null) {
                throw new IllegalStateException("Couldn't determine CPU family from powermetrics output");
            }

            final var metadata = new SensorMetadata(components, "macOS powermetrics derived information, see https://firefox-source-docs.mozilla.org/performance/powermetrics.html");
            cpu.setMetadata(metadata);
            return cpu;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SensorMetadata metadata() {
        return cpu.metadata();
    }

    private static class ProcessRecord {
        final double cpu;
        final double gpu;
        final String pid;
        private static final Pattern spaces = Pattern.compile("\\s+");

        public ProcessRecord(String line) throws IllegalArgumentException {
            //Name                               ID     CPU ms/s  samp ms/s  User%  Deadlines (<2 ms, 2-5 ms)  Wakeups (Intr, Pkg idle)  GPU ms/s
            //iTerm2                             1008   46.66     46.91      83.94  0.00    0.00               30.46   0.00              0.00
            final var processData = spaces.split(line, 10);
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
        return measures.register(pid);
    }

    Measures extractPowerMeasure(InputStream powerMeasureInput, Long tick) {
        try {
            // Should not be closed since it closes the process
            BufferedReader input = new BufferedReader(new InputStreamReader(powerMeasureInput));
            String line;

            double totalSampledCPU = -1;
            double totalSampledGPU = -1;
            int headerLinesToSkip = 10;
            // copy the pids so that we can remove them as soon as we've processed them
            final var pidsToProcess = new HashSet<>(measures.trackedPIDs());
            // start measure
            final var pidMeasures = new HashMap<RegisteredPID, ProcessRecord>(measures.numberOfTrackerPIDs());
            final var metadata = cpu.metadata();
            final var powerComponents = new HashMap<String, Integer>(metadata.componentCardinality());
            while ((line = input.readLine()) != null) {
                if (headerLinesToSkip != 0) {
                    headerLinesToSkip--;
                    continue;
                }

                if (line.isEmpty() || line.charAt(0) == '*') {
                    continue;
                }

                // first, look for process line detailing share
                if (!pidsToProcess.isEmpty()) {
                    for (RegisteredPID pid : pidsToProcess) {
                        if (line.contains(pid.stringForMatching())) {
                            pidMeasures.put(pid, new ProcessRecord(line));
                            pidsToProcess.remove(pid);
                            break;
                        }
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
                if (powerComponents.size() == metadata.componentCardinality() - 1) {
                    break;
                }
            }

            final var hasGPU = totalSampledGPU != 0;
            double finalTotalSampledGPU = totalSampledGPU;
            double finalTotalSampledCPU = totalSampledCPU;
            pidMeasures.forEach((pid, record) -> {
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
                });

                measures.record(pid, new SensorMeasure(measure, tick));
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return measures;
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
            powermetrics = new ProcessBuilder().command("sudo", "powermetrics", "--samplers", "cpu_power,tasks", "--show-process-samp-norm", "--show-process-gpu", "-i", freq).start();
        }
    }

    @Override
    public boolean isStarted() {
        return powermetrics != null && powermetrics.isAlive();
    }

    @Override
    public Measures update(Long tick) {
        return extractPowerMeasure(powermetrics.getInputStream(), tick);
    }

    @Override
    public void stop() {
        powermetrics.destroy();
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
        // if we're not tracking any processes anymore, stop powermetrics as well
        if (measures.numberOfTrackerPIDs() == 0) {
            stop();
        }
    }
}
