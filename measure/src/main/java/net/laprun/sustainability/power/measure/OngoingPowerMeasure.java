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
    private final BitSet nonZeroComponents;
    private final List<RegisteredSyntheticComponent> syntheticComponents;
    private final long samplePeriod;
    private double[][] measures;
    private long startedAt;
    private int samples;
    private long[] timestamps;

    public OngoingPowerMeasure(SensorMetadata metadata, SyntheticComponent... syntheticComponents) {
        this(metadata, -1, syntheticComponents);
    }

    public OngoingPowerMeasure(SensorMetadata metadata, long samplePeriod, SyntheticComponent... syntheticComponents) {
        super(Processors.empty);

        final int componentCardinality = metadata.componentCardinality();
        nonZeroComponents = new BitSet(componentCardinality);
        reset(componentCardinality);
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

    public void reset() {
        reset(metadata.componentCardinality());
    }

    private synchronized void reset(int componentCardinality) {
        startedAt = System.currentTimeMillis();
        nonZeroComponents.clear();
        measures = new double[componentCardinality][DEFAULT_SIZE];
        timestamps = new long[DEFAULT_SIZE];
        samples = 0;
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
        final double[] measuresForComponent = getLiveMeasuresFor(component);
        if (measuresForComponent == null || measuresForComponent.length == 0) {
            return DoubleStream.empty();
        }
        return Arrays.stream(measuresForComponent, 0, samples);
    }

    public double[] getLiveMeasuresFor(int component) {
        if (nonZeroComponents.get(component)) {
            return measures[component];
        } else {
            final var match = syntheticComponents.stream()
                    .filter(rsc -> targetComponentExistsAndIsRecorder(component, rsc))
                    .map(rsc -> (Recorder) rsc.syntheticComponent())
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                return match.liveMeasures();
            }
        }
        return new double[0];
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

    public Cursor.PartialCursor getCursorOver(long timestamp, Duration duration) {
        return Cursor.cursorOver(timestamps, timestamp, duration, startedAt, samplePeriod);
    }
}
