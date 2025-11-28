package net.laprun.sustainability.power.sensors.macos.powermetrics;

import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.CPU_SHARE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.measures.NoDurationSensorMeasure;
import net.laprun.sustainability.power.measures.PartialSensorMeasure;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.RegisteredPID;

public enum PowerMetricsParser {
    ;

    private static final String DURATION_SUFFIX = "ms elapsed) ***";
    private static final int DURATION_SUFFIX_LENGTH = DURATION_SUFFIX.length();
    private static final String TASKS_SECTION_MARKER = "*** Running tasks ***";
    private static final String CPU_USAGE_SECTION_MARKER = "**** Processor usage ****";

    static CPU initCPU(InputStream inputStream) {
        CPU cpu = null;
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

        return cpu;
    }

    static void extractPowerMeasure(InputStream powerMeasureInput, Measures current, long lastUpdateEpoch, long newUpdateEpoch,
            Set<RegisteredPID> registeredPIDs, SensorMetadata metadata, CPU cpu) {
        final long startMs = lastUpdateEpoch;
        try {
            Log.infof("Parsing: %s", powerMeasureInput);
            // Should not be closed since it closes the process
            BufferedReader input = new BufferedReader(new InputStreamReader(powerMeasureInput));
            String line;

            double totalSampledCPU = -1;
            double totalSampledGPU = -1;
            // copy the pids so that we can remove them as soon as we've processed them
            final var pidsToProcess = new HashSet<>(registeredPIDs);
            // remove total system "pid"
            pidsToProcess.remove(RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID);
            // start measure
            final var pidMeasures = new HashMap<RegisteredPID, ProcessRecord>(registeredPIDs.size());
            final var powerComponents = new HashMap<String, Number>(metadata.componentCardinality());
            var duration = -1L;
            Section processes = null;
            Section cpuSection = null;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                if (line.charAt(0) == '*') {
                    // check if we have the sample duration
                    if (duration == -1 && line.endsWith(DURATION_SUFFIX)) {
                        final var startLookingIndex = line.length() - DURATION_SUFFIX_LENGTH;
                        final var lastOpenParenIndex = line.lastIndexOf('(', startLookingIndex);
                        if (lastOpenParenIndex > 0) {
                            duration = Math.round(Float.parseFloat(line.substring(lastOpenParenIndex + 1, startLookingIndex)));
                        }
                        continue;
                    }

                    // check for the beginning of tasks section
                    if (processes == null && line.equals(TASKS_SECTION_MARKER)) {
                        // make flag null to indicate it needs to be set to true on the next iteration, to avoid processing the marker line for processes
                        processes = new Section();
                        continue;
                    }

                    if (cpuSection == null && line.equals(CPU_USAGE_SECTION_MARKER)) {
                        cpuSection = new Section();
                        continue;
                    }

                    continue;
                }

                // first, look for process line detailing share
                if (processes != null && !processes.done && !pidsToProcess.isEmpty()) {
                    if (line.startsWith("ALL_TASKS")) {
                        processes.done = true; // we reached the end of the process section
                    } else {
                        for (RegisteredPID pid : pidsToProcess) {
                            if (line.contains(pid.stringForMatching())) {
                                pidMeasures.put(pid, new ProcessRecord(line));
                                pidsToProcess.remove(pid);
                                break;
                            }
                        }
                    }
                }

                // then skip all lines until we get the totals
                if (totalSampledCPU < 0 && line.startsWith("ALL_TASKS")) {
                    final var totals = new ProcessRecord(line);
                    // compute ratio
                    totalSampledCPU = totals.cpu;
                    totalSampledGPU = totals.gpu > 0 ? totals.gpu : 0;
                    if (!pidsToProcess.isEmpty()) {
                        Log.warnf("Couldn't find processes: %s",
                                Arrays.toString(pidsToProcess.stream().map(RegisteredPID::pid).toArray(Long[]::new)));
                    }
                }

                // we need an exit condition to break out of the loop, otherwise we'll just keep looping forever since there are always new lines since the process is periodical
                // fixme: perhaps we should relaunch the process on each update loop instead of keeping it running? Not sure which is more efficient
                if (cpuSection != null && !cpuSection.done && cpu.doneExtractingPowerComponents(line, powerComponents)) {
                    break;
                }
            }

            double finalTotalSampledGPU = totalSampledGPU;
            double finalTotalSampledCPU = totalSampledCPU;
            final var endMs = newUpdateEpoch;
            final var durationMs = duration;

            // handle total system measure separately
            final var systemTotalMeasure = getSystemTotalMeasure(metadata, powerComponents);
            recordMeasure(RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID, current, systemTotalMeasure, startMs, endMs, durationMs);

            pidMeasures.forEach((pid, record) -> {
                final var attributedMeasure = record.asAttributedMeasure(metadata, powerComponents, finalTotalSampledCPU,
                        finalTotalSampledGPU);
                recordMeasure(pid, current, attributedMeasure, startMs, endMs, durationMs);
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void recordMeasure(RegisteredPID pid, Measures current, double[] components, long startMs, long endMs,
            long duration) {
        current.record(pid, duration > 0 ? new PartialSensorMeasure(components, startMs, endMs, duration)
                : new NoDurationSensorMeasure(components, startMs, endMs));
    }

    private static double[] getSystemTotalMeasure(SensorMetadata metadata, Map<String, Number> powerComponents) {
        final var measure = new double[metadata.componentCardinality()];
        metadata.components().forEach((name, cm) -> {
            final var index = cm.index();
            final var value = CPU_SHARE.equals(name) ? 1.0
                    : powerComponents.getOrDefault(name, 0).doubleValue();
            measure[index] = value;
        });

        return measure;
    }

    private static class Section {
        boolean done;
    }
}
