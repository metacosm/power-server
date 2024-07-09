package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final MeasureStore measures;
    private final long startedAt;
    private final double[] averages;
    private final Set<Integer> nonZeroComponents;
    private final int[] totalComponents;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private int samples;

    public OngoingPowerMeasure(SensorMetadata sensorMetadata, Duration duration, Duration frequency) {
        this.sensorMetadata = sensorMetadata;
        startedAt = System.currentTimeMillis();
        final var numComponents = metadata().componentCardinality();
        averages = new double[numComponents];

        final var initialWindow = (int) (duration.toMillis() / frequency.toMillis());
        measures = new DescriptiveStatisticsMeasureStore(numComponents, initialWindow);

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
            measures.recordComponentValue(component, componentValue);
            averages[component] = averages[component] == 0 ? componentValue
                    : (previousSize * averages[component] + componentValue) / samples;
        }

        // record min / max totals
        final var recordedTotal = PowerMeasure.sumOfSelectedComponents(components, totalComponents);
        measures.recordTotal(recordedTotal);
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }

    @Override
    public double total() {
        return measures.getMeasuredTotal();
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
                .forEach(component -> stdDevs[component] = measures.getComponentStandardDeviation(component));

        return new StdDev(measures.getTotalStandardDeviation(), stdDevs);
    }

    @Override
    public double[] getMeasuresFor(int component) {
        return measures.getComponentRawValues(component);
    }
}
