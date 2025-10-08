package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.AbstractPowerSensor;
import net.laprun.sustainability.power.sensors.MapMeasures;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.RegisteredPID;

/**
 * A macOS powermetrics based {@link PowerSensor} implementation.
 */
public abstract class MacOSPowermetricsSensor extends AbstractPowerSensor<MapMeasures> {
    /**
     * The Central Processing Unit component name
     */
    public static final String CPU = "CPU";
    /**
     * The Graphics Procssing Unit component name
     */
    public static final String GPU = "GPU";
    /**
     * The Apple Neural Engine component name
     */
    public static final String ANE = "ANE";
    /**
     * The Dynamic Random Access Memory component name
     */
    @SuppressWarnings("unused")
    public static final String DRAM = "DRAM";
    @SuppressWarnings("unused")
    public static final String DCS = "DCS";
    /**
     * The package component name
     */
    public static final String PACKAGE = "Package";
    /**
     * The extracted CPU share component name, this represents the process' share of the measured power consumption
     */
    public static final String CPU_SHARE = "cpuShare";

    private CPU cpu;

    public MacOSPowermetricsSensor() {
        super(new MapMeasures());
    }

    void initMetadata(InputStream inputStream) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            var components = new ArrayList<SensorMetadata.ComponentMetadata>();
            while ((line = input.readLine()) != null) {
                if (cpu == null) {
                    // if we reached the OS line while cpu is still null, we're looking at an Apple Silicon CPU
                    if (line.startsWith("OS ")) {
                        cpu = new AppleSiliconCPU();
                    } else if (line.startsWith("EFI ")) {
                        cpu = new IntelCPU();
                    }

                    if (cpu != null && cpu.doneAfterComponentsInitialization(components)) {
                        break;
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

            final var metadata = new SensorMetadata(components,
                    "macOS powermetrics derived information, see https://firefox-source-docs.mozilla.org/performance/powermetrics.html");
            cpu.setMetadata(metadata);
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

        public ProcessRecord(String line) throws IllegalArgumentException {
            // Expected normal output:
            //Name                               ID     CPU ms/s  samp ms/s  User%  Deadlines (<2 ms, 2-5 ms)  Wakeups (Intr, Pkg idle)  GPU ms/s
            //iTerm2                             1008   46.66     46.91      83.94  0.00    0.00               30.46   0.00              0.00
            // Expected summary output:
            //Name                               ID     CPU ms/s  samp ms/s [total]     User%  Deadlines/s [total] (<2 ms, 2-5 ms)  Wakeups/s [total] (Intr, Pkg idle)  Dead  GPU ms/s
            //WindowServer                       406    493.74    493.96    [5165.88  ] 64.82  65.95   [690    ] 0.00    [0      ]  656.62  [6870   ] 4.21    [44     ] N     0.00

            try {
                // Trim leading/trailing whitespace
                line = line.trim();

                // Find first whitespace block after process name (marks start of ID)
                int idStart = findFirstWhitespace(line);
                if (idStart == -1) {
                    throw new IllegalArgumentException("Cannot find ID in line: " + line);
                }

                // Skip whitespace to get to ID
                idStart = skipWhitespace(line, idStart);
                int idEnd = findNextWhitespace(line, idStart);
                pid = RegisteredPID.prepare(line.substring(idStart, idEnd));

                // Skip CPU ms/s column (skip whitespace, then number, then whitespace)
                int pos = skipWhitespace(line, idEnd);
                pos = skipNumber(line, pos);

                // Now at samp ms/s
                pos = skipWhitespace(line, pos);
                int sampStart = pos;
                int sampEnd = skipNumber(line, sampStart);
                cpu = Double.parseDouble(line.substring(sampStart, sampEnd));

                // Skip to end and work backwards to find GPU ms/s
                // The GPU value is the last numeric value on the line
                int lastNumEnd = line.length();
                while (lastNumEnd > 0 && Character.isWhitespace(line.charAt(lastNumEnd - 1))) {
                    lastNumEnd--;
                }

                int lastNumStart = lastNumEnd;
                while (lastNumStart > 0 && isNumberChar(line.charAt(lastNumStart - 1))) {
                    lastNumStart--;
                }

                if (lastNumStart < lastNumEnd) {
                    gpu = Double.parseDouble(line.substring(lastNumStart, lastNumEnd));
                } else {
                    throw new IllegalArgumentException("Cannot find GPU value in line: " + line);
                }

            } catch (Exception e) {
                throw new IllegalArgumentException("Received line doesn't conform to expected format: " + line, e);
            }
        }

        private static int findFirstWhitespace(String line) {
            for (int i = 0; i < line.length(); i++) {
                if (Character.isWhitespace(line.charAt(i))) {
                    return i;
                }
            }
            return -1;
        }

        private static int skipWhitespace(String line, int pos) {
            while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
                pos++;
            }
            return pos;
        }

        private static int findNextWhitespace(String line, int pos) {
            while (pos < line.length() && !Character.isWhitespace(line.charAt(pos))) {
                pos++;
            }
            return pos;
        }

        private static int skipNumber(String line, int pos) {
            while (pos < line.length() && isNumberChar(line.charAt(pos))) {
                pos++;
            }
            return pos;
        }

        private static boolean isNumberChar(char c) {
            return Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E';
        }
    }

    Measures extractPowerMeasure(InputStream powerMeasureInput, Long tick) {
        final long start = System.currentTimeMillis();
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
            final var pidMeasures = new HashMap<RegisteredPID, ProcessRecord>(measures.numberOfTrackedPIDs());
            final var metadata = cpu.metadata();
            final var powerComponents = new HashMap<String, Number>(metadata.componentCardinality());
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

                        // todo? if pid is not found, this will loop forever and we should break if ALL_TASKS is reached without draining the pids to process
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

                // we need an exit condition to break out of the loop, otherwise we'll just keep looping forever since there are always new lines since the process is periodical
                // fixme: perhaps we should relaunch the process on each update loop instead of keeping it running? Not sure which is more efficient
                if (cpu.doneExtractingPowerComponents(line, powerComponents)) {
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
                    final var value = CPU_SHARE.equals(name) ? cpuShare : powerComponents.getOrDefault(name, 0).doubleValue();

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

                measures.record(pid, new SensorMeasure(measure, start, System.currentTimeMillis()));
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return measures;
    }

    @Override
    public Measures update(Long tick) {
        return extractPowerMeasure(getInputStream(), tick);
    }

    protected abstract InputStream getInputStream();

    @Override
    public void unregister(RegisteredPID registeredPID) {
        super.unregister(registeredPID);
        // if we're not tracking any processes anymore, stop powermetrics as well
        if (measures.numberOfTrackedPIDs() == 0) {
            stop();
        }
    }
}
