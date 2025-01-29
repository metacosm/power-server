package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.DoubleStream;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.Processors;
import net.laprun.sustainability.power.analysis.Recorder;
import net.laprun.sustainability.power.analysis.RegisteredSyntheticComponent;
import net.laprun.sustainability.power.analysis.SyntheticComponent;

public class OngoingPowerMeasure extends ProcessorAware implements PowerMeasure {
    private static final int DEFAULT_SIZE = 32;
    private final SensorMetadata metadata;
    private final long startedAt;
    private final BitSet nonZeroComponents;
    private final double[][] measures;
    private final List<RegisteredSyntheticComponent> syntheticComponents;
    private int samples;
    private long[] timestamps;
    private long samplePeriod;

    public OngoingPowerMeasure(SensorMetadata metadata, SyntheticComponent... syntheticComponents) {
        this(metadata, -1, syntheticComponents);
    }

    public OngoingPowerMeasure(SensorMetadata metadata, long samplePeriod, SyntheticComponent... syntheticComponents) {
        super(Processors.empty);

        startedAt = System.currentTimeMillis();
        final var numComponents = metadata.componentCardinality();
        measures = new double[numComponents][DEFAULT_SIZE];
        nonZeroComponents = new BitSet(numComponents);
        timestamps = new long[DEFAULT_SIZE];
        this.samplePeriod = samplePeriod;

        if (syntheticComponents != null) {
            final var builder = SensorMetadata.from(metadata);
            for (var component : syntheticComponents) {
                builder.withNewComponent(component.metadata());
            }
            this.metadata = builder.build();
            this.syntheticComponents = Arrays.stream(syntheticComponents)
                    .map(sc -> new RegisteredSyntheticComponent(sc, this.metadata.metadataFor(sc.metadata().name()).index()))
                    .toList();
        } else {
            this.syntheticComponents = List.of();
            this.metadata = metadata;
        }
    }

    @Override
    public synchronized int numberOfSamples() {
        return samples;
    }

    @Override
    public SensorMetadata metadata() {
        return metadata;
    }

    public void recordMeasure(double[] components) {
        ensureArraysSize();

        final var timestamp = System.currentTimeMillis();

        synchronized (this) {
            timestamps[samples - 1] = timestamp;
            for (int component = 0; component < components.length; component++) {
                final var componentValue = components[component];
                // record that the value is not zero
                if (componentValue != 0) {
                    nonZeroComponents.set(component);
                }
                measures[component][samples - 1] = componentValue;
            }
        }

        final var processors = processors();
        processors.recordMeasure(components, timestamp);

        if (!syntheticComponents.isEmpty()) {
            syntheticComponents.forEach(sc -> processors.recordSyntheticComponentValue(sc.synthesizeFrom(components, timestamp),
                    timestamp, sc.computedIndex()));

        }
    }

    private synchronized void ensureArraysSize() {
        samples++;
        final int currentSize = timestamps.length;
        if (currentSize <= samples) {
            final var newSize = currentSize * 2;
            for (int index = 0; index < measures.length; index++) {
                final var newArray = new double[newSize];
                System.arraycopy(measures[index], 0, newArray, 0, currentSize);
                measures[index] = newArray;
            }
            final var newTimestamps = new long[newSize];
            System.arraycopy(timestamps, 0, newTimestamps, 0, currentSize);
            timestamps = newTimestamps;
        }
    }

    public Duration duration() {
        return Duration.ofMillis(System.currentTimeMillis() - startedAt);
    }

    @Override
    public DoubleStream getMeasuresFor(int component) {
        if (nonZeroComponents.get(component)) {
            return Arrays.stream(measures[component], 0, samples);
        } else {
            final var match = syntheticComponents.stream()
                    .filter(rsc -> targetComponentExistsAndIsRecorder(component, rsc))
                    .map(rsc -> (Recorder) rsc.syntheticComponent())
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match.measures();
            }
        }
        return DoubleStream.empty();
    }

    private static boolean targetComponentExistsAndIsRecorder(int component, RegisteredSyntheticComponent rsc) {
        return component == rsc.computedIndex() && rsc.syntheticComponent() instanceof Recorder;
    }

    @Override
    public synchronized TimestampedMeasures getNthTimestampedMeasures(int n) {
        n = Math.min(n, samples - 1);
        final var result = new double[measures.length];
        for (int i = 0; i < measures.length; i++) {
            result[i] = measures[i][n];
        }
        return new TimestampedMeasures(timestamps[n], result);
    }
}
