package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final DescriptiveStatistics[] measures;
    private final DescriptiveStatistics total;
    private final long startedAt;
    private final double[] averages;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private int samples;

    public OngoingPowerMeasure(SensorMetadata sensorMetadata, Duration duration, Duration frequency) {
        this.sensorMetadata = sensorMetadata;
        startedAt = System.currentTimeMillis();
        final var numComponents = metadata().componentCardinality();
        averages = new double[numComponents];

        final var initialWindow = (int) (duration.toMillis() / frequency.toMillis());
        total = new DescriptiveStatistics(initialWindow);
        this.measures = new DescriptiveStatistics[sensorMetadata.componentCardinality()];
        for (int i = 0; i < measures.length; i++) {
            measures[i] = new DescriptiveStatistics(initialWindow);
        }
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
            measures[component].addValue(componentValue);
            averages[component] = averages[component] == 0 ? componentValue
                    : (previousSize * averages[component] + componentValue) / samples;
        }

        // record min / max totals
        final var recordedTotal = PowerMeasure.sumOfSelectedComponents(components, metadata().totalComponents());
        total.addValue(recordedTotal);
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }

    @Override
    public double total() {
        return total.getSum();
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
        IntStream.range(0, cardinality)
                .parallel()
                .forEach(component -> stdDevs[component] = measures[component].getStandardDeviation());

        return new StdDev(total.getStandardDeviation(), stdDevs);
    }

    @Override
    public double[] getMeasuresFor(int component) {
        return measures[component].getValues();
    }
}
