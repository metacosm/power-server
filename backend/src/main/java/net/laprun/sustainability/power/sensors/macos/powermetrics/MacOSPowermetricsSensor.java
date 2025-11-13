package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.quarkus.logging.Log;
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
    private static final String DURATION_SUFFIX = "ms elapsed) ***";
    private static final int DURATION_SUFFIX_LENGTH = DURATION_SUFFIX.length();
    public static final String TASKS_SECTION_MARKER = "*** Running tasks ***";
    public static final String CPU_USAGE_SECTION_MARKER = "**** Processor usage ****";

    private CPU cpu;

    public MacOSPowermetricsSensor() {
        super(new MapMeasures());
    }

    @Override
    public boolean supportsProcessAttribution() {
        return true;
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
    protected SensorMetadata nativeMetadata() {
        return cpu.metadata();
    }

    @Override
    protected void doStart(long samplingFrequencyInMillis) {
        // nothing to do here by default
    }

    private static class Section {
        boolean done;
    }

    Measures extractPowerMeasure(InputStream powerMeasureInput, long lastUpdateEpoch, long newUpdateEpoch,
            Map<String, Double> cpuShares) {
        final long start = lastUpdateEpoch;
        try {
            // Should not be closed since it closes the process
            BufferedReader input = new BufferedReader(new InputStreamReader(powerMeasureInput));
            String line;

            double totalSampledCPU = -1;
            double totalSampledGPU = -1;
            // copy the pids so that we can remove them as soon as we've processed them
            final var pidsToProcess = new HashSet<>(measures.trackedPIDs());
            // remove total system "pid"
            pidsToProcess.remove(RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID);
            // start measure
            final var pidMeasures = new HashMap<RegisteredPID, ProcessRecord>(measures.numberOfTrackedPIDs());
            final var metadata = metadata();
            final var powerComponents = new HashMap<String, Number>(metadata.componentCardinality());
            var endUpdateEpoch = -1L;
            Section processes = null;
            Section cpuSection = null;
            while ((line = input.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                if (line.charAt(0) == '*') {
                    // check if we have the sample duration
                    if (endUpdateEpoch == -1 && line.endsWith(DURATION_SUFFIX)) {
                        final var startLookingIndex = line.length() - DURATION_SUFFIX_LENGTH;
                        final var lastOpenParenIndex = line.lastIndexOf('(', startLookingIndex);
                        if (lastOpenParenIndex > 0) {
                            endUpdateEpoch = start
                                    + Math.round(Float.parseFloat(line.substring(lastOpenParenIndex + 1, startLookingIndex)));
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
            final var endMs = endUpdateEpoch != -1 ? endUpdateEpoch : newUpdateEpoch;

            // handle external cpu share if enabled
            if (cpuShares != null && !cpuShares.isEmpty()) {
                measures.trackedPIDsAsString().forEach(name -> powerComponents.put(EXTERNAL_CPU_SHARE_COMPONENT_NAME,
                        cpuShares.getOrDefault(name, MISSING_CPU_SHARE)));
            }

            // handle total system measure separately
            measures.record(RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID,
                    new SensorMeasure(getSystemTotalMeasure(metadata, powerComponents), start, endMs));

            pidMeasures.forEach((pid, record) -> {
                final var attributedMeasure = record.asAttributedMeasure(metadata, powerComponents, finalTotalSampledCPU,
                        finalTotalSampledGPU);
                measures.record(pid, new SensorMeasure(attributedMeasure, start, endMs));
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return measures;
    }

    private static double[] getSystemTotalMeasure(SensorMetadata metadata, Map<String, Number> powerComponents) {
        final var measure = new double[metadata.componentCardinality()];
        metadata.components().forEach((name, cm) -> {
            final var index = cm.index();
            final var value = CPU_SHARE.equals(name) || EXTERNAL_CPU_SHARE_COMPONENT_NAME.equals(name) ? 1.0
                    : powerComponents.getOrDefault(name, 0).doubleValue();
            measure[index] = value;
        });

        return measure;
    }

    @Override
    protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch, Map<String, Double> cpuShares) {
        return extractPowerMeasure(getInputStream(), lastUpdateEpoch, newUpdateStartEpoch, cpuShares);
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
