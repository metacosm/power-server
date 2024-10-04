package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final ComponentMeasure[] measures;
    private final long startedAt;
    private final double[] averages;
    private final Set<Integer> nonZeroComponents;
    private final int[] totalComponents;
    private final int totalIndex;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private double accumulatedTotal;
    private int samples;

    public OngoingPowerMeasure(SensorMetadata sensorMetadata, ComponentMeasure.Factory<?> componentMeasureFactory) {
        this.sensorMetadata = sensorMetadata;
        startedAt = System.currentTimeMillis();

        final var numComponents = sensorMetadata.componentCardinality();
        // we also record the aggregated total for each component participating in the aggregated value
        final var measuresNb = numComponents + 1;
        measures = new ComponentMeasure[measuresNb];
        for (int i = 0; i < measuresNb; i++) {
            measures[i] = componentMeasureFactory.create();
        }
        totalIndex = numComponents;
        averages = new double[measuresNb];
        // we don't need to record the total component as a non-zero component since it's almost never zero and we compute the std dev separately
        nonZeroComponents = new HashSet<>(numComponents);
        totalComponents = sensorMetadata.totalComponents();
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
        final var previousSize = samples;
        samples++;
        for (int component = 0; component < components.length; component++) {
            final var componentValue = components[component];
            // record that the value is not zero
            if (componentValue != 0) {
                nonZeroComponents.add(component);
            }
            measures[component].recordComponentValue(componentValue);
            averages[component] = averages[component] == 0 ? componentValue
                    : (previousSize * averages[component] + componentValue) / samples;
        }

        // record min / max totals
        final var recordedTotal = PowerMeasure.sumOfSelectedComponents(components, totalComponents);
        measures[totalIndex].recordComponentValue(recordedTotal);
        accumulatedTotal += recordedTotal;
        averages[components.length] = averages[components.length] == 0 ? recordedTotal
                : (previousSize * averages[components.length] + recordedTotal) / samples;
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
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
    public double[] averagesPerComponent() {
        return averages;
    }

    public StdDev standardDeviations() {
        final var cardinality = sensorMetadata.componentCardinality();
        final var stdDevs = new double[cardinality];
        nonZeroComponents.stream()
                .parallel()
                .forEach(component -> stdDevs[component] = standardDeviation(component));

        final double aggregate = maxTotal == 0 ? 0 : standardDeviation(totalIndex);
        return new StdDev(aggregate, stdDevs);
    }

    private double standardDeviation(int component) {
        final var values = measures[component].getComponentRawValues();
        if (samples <= 1) {
            return 0.0;
        }
        final double mean = averages[component];
        double geometric_deviation_total = 0.0;
        for (double value : values) {
            double deviation = value - mean;
            geometric_deviation_total += (deviation * deviation);
        }
        return FastMath.sqrt(geometric_deviation_total / (samples - 1));
    }

    @Override
    public double[] getMeasuresFor(int component) {
        return measures[component].getComponentRawValues();
    }
}
