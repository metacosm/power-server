package net.laprun.sustainability.power.measure;

import java.time.Duration;

import net.laprun.sustainability.power.SensorMetadata;

@SuppressWarnings("unused")
public class StoppedPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final int samples;
    private final Duration duration;
    private final double total;
    private final double min;
    private final double max;
    private final double[] averages;
    private final StdDev standardDeviations;
    private final double[][] measures;

    public StoppedPowerMeasure(PowerMeasure powerMeasure) {
        this.sensorMetadata = powerMeasure.metadata();
        this.duration = powerMeasure.duration();
        this.total = powerMeasure.total();
        this.min = powerMeasure.minMeasuredTotal();
        this.max = powerMeasure.maxMeasuredTotal();
        this.averages = powerMeasure.averagesPerComponent();
        this.standardDeviations = powerMeasure.standardDeviations();
        this.samples = powerMeasure.numberOfSamples();
        final var cardinality = metadata().componentCardinality();
        measures = new double[cardinality][samples];
        for (int i = 0; i < cardinality; i++) {
            measures[i] = powerMeasure.getMeasuresFor(i);
        }
    }

    @Override
    public Duration duration() {
        return duration;
    }

    @Override
    public double minMeasuredTotal() {
        return min;
    }

    @Override
    public double maxMeasuredTotal() {
        return max;
    }

    @Override
    public double total() {
        return total;
    }

    @Override
    public double[] averagesPerComponent() {
        return averages;
    }

    @Override
    public int numberOfSamples() {
        return samples;
    }

    @Override
    public SensorMetadata metadata() {
        return sensorMetadata;
    }

    @Override
    public StdDev standardDeviations() {
        return standardDeviations;
    }

    @Override
    public double[] getMeasuresFor(int component) {
        return measures[component];
    }
}
