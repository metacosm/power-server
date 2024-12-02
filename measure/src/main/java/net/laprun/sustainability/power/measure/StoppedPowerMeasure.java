package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Optional;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;

@SuppressWarnings("unused")
public class StoppedPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final int samples;
    private final Duration duration;
    private final double total;
    private final double min;
    private final double max;
    private final double[][] measures;
    private final ComponentProcessor[] processors;

    public StoppedPowerMeasure(PowerMeasure powerMeasure) {
        this.sensorMetadata = powerMeasure.metadata();
        this.duration = powerMeasure.duration();
        this.total = powerMeasure.total();
        this.min = powerMeasure.minMeasuredTotal();
        this.max = powerMeasure.maxMeasuredTotal();
        this.samples = powerMeasure.numberOfSamples();
        final var cardinality = metadata().componentCardinality();
        measures = new double[cardinality][samples];
        for (int i = 0; i < cardinality; i++) {
            measures[i] = powerMeasure.getMeasuresFor(i).orElse(null);
        }
        processors = powerMeasure.analyzers();
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
    public int numberOfSamples() {
        return samples;
    }

    @Override
    public SensorMetadata metadata() {
        return sensorMetadata;
    }

    @Override
    public Optional<double[]> getMeasuresFor(int component) {
        return Optional.ofNullable(measures[component]);
    }

    @Override
    public ComponentProcessor[] analyzers() {
        return processors;
    }
}
