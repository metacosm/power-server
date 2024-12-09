package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.BitSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;

public class OngoingPowerMeasure implements PowerMeasure {
    private static final int DEFAULT_SIZE = 32;
    private final SensorMetadata sensorMetadata;
    private final long startedAt;
    private final BitSet nonZeroComponents;
    private final int[] totalComponents;
    private final int totalIndex;
    private final double[][] measures;
    private final ComponentProcessor[] analyzers;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private double accumulatedTotal;
    private int samples;
    private long[] timestamps;

    public OngoingPowerMeasure(SensorMetadata sensorMetadata, ComponentProcessor... analyzers) {
        this.sensorMetadata = sensorMetadata;
        startedAt = System.currentTimeMillis();

        final var numComponents = sensorMetadata.componentCardinality();
        // we also record the aggregated total for each component participating in the aggregated value
        final var measuresNb = numComponents + 1;
        measures = new double[measuresNb][DEFAULT_SIZE];
        timestamps = new long[DEFAULT_SIZE];
        totalIndex = numComponents;
        // we don't need to record the total component as a non-zero component since it's almost never zero and we compute the std dev separately
        nonZeroComponents = new BitSet(numComponents);
        totalComponents = sensorMetadata.totalComponents();
        this.analyzers = Objects.requireNonNullElseGet(analyzers, () -> new ComponentProcessor[0]);
    }

    @Override
    public int numberOfSamples() {
        return samples;
    }

    @Override
    public SensorMetadata metadata() {
        return sensorMetadata;
    }

    public void recordMeasure(double[] components) {
        samples++;
        for (int component = 0; component < components.length; component++) {
            final var componentValue = components[component];
            // record that the value is not zero
            if (componentValue != 0) {
                nonZeroComponents.set(component);
            }
            recordComponentValue(component, componentValue);
        }

        // record min / max totals
        final var recordedTotal = Compute.sumOfSelectedComponents(components, totalComponents);
        recordComponentValue(totalIndex, recordedTotal);
        accumulatedTotal += recordedTotal;
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }

    private void recordComponentValue(int component, double value) {
        final var currentSize = measures[component].length;
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
        final var timestamp = System.currentTimeMillis();
        timestamps[component] = timestamp;
        measures[component][samples - 1] = value;
        for (var analyzer : analyzers) {
            analyzer.recordComponentValue(value, timestamp);
        }
    }

    @Override
    public double total() {
        return accumulatedTotal;
    }

    public Duration duration() {
        return Duration.ofMillis(System.currentTimeMillis() - startedAt);
    }

    @Override
    public double minMeasuredTotal() {
        return minTotal;
    }

    @Override
    public double maxMeasuredTotal() {
        return maxTotal;
    }

    @Override
    public Optional<double[]> getMeasuresFor(int component) {
        return measuresFor(component, samples);
    }

    Optional<double[]> measuresFor(int component, int upToIndex) {
        if (nonZeroComponents.get(component)) {
            final var dest = new double[upToIndex];
            System.arraycopy(measures[component], 0, dest, 0, upToIndex);
            return Optional.of(dest);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<TimestampedValue> streamTimestampedMeasuresFor(int component, int upToIndex) {
        final var componentMeasures = measures[component];
        return indicesFor(component, upToIndex)
                .mapToObj(index -> new TimestampedValue(timestamps[index], componentMeasures[index]));
    }

    @Override
    public DoubleStream streamMeasuresFor(int component, int upToIndex) {
        final var componentMeasures = measures[component];
        return indicesFor(component, upToIndex).mapToDouble(index -> componentMeasures[index]);
    }

    IntStream indicesFor(int component, int upToIndex) {
        upToIndex = Math.min(upToIndex, samples - 1);
        if (upToIndex >= 0 && nonZeroComponents.get(component)) {
            return IntStream.range(0, upToIndex);
        } else {
            return IntStream.empty();
        }
    }

    @Override
    public TimestampedMeasures getNthTimestampedMeasures(int n) {
        n = Math.min(n, samples - 1);
        final var result = new double[measures.length];
        for (int i = 0; i < measures.length; i++) {
            result[i] = measures[i][n];
        }
        return new TimestampedMeasures(timestamps[n], result);
    }

    @Override
    public ComponentProcessor[] analyzers() {
        return analyzers;
    }
}
