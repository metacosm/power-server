package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import net.laprun.sustainability.power.SensorMetadata;

@SuppressWarnings("unused")
public class StoppedPowerMeasure extends ProcessorAware implements PowerMeasure {
    private final OngoingPowerMeasure measure;
    private final int samples;
    private final Duration duration;

    public StoppedPowerMeasure(OngoingPowerMeasure powerMeasure) {
        super(powerMeasure.processors());

        this.measure = powerMeasure;
        this.duration = powerMeasure.duration();
        this.samples = powerMeasure.numberOfSamples();
        final var cardinality = metadata().componentCardinality();
    }

    @Override
    public Duration duration() {
        return duration;
    }

    @Override
    public int numberOfSamples() {
        return samples;
    }

    @Override
    public SensorMetadata metadata() {
        return measure.metadata();
    }

    @Override
    public Optional<double[]> getMeasuresFor(int component) {
        return measure.measuresFor(component, samples);
    }

    @Override
    public Stream<TimestampedValue> streamTimestampedMeasuresFor(int component, int upToIndex) {
        upToIndex = ensureIndex(upToIndex);
        return measure.streamTimestampedMeasuresFor(component, upToIndex);
    }

    @Override
    public DoubleStream streamMeasuresFor(int component, int upToIndex) {
        upToIndex = ensureIndex(upToIndex);
        return measure.streamMeasuresFor(component, upToIndex);
    }

    @Override
    public TimestampedMeasures getNthTimestampedMeasures(int n) {
        n = ensureIndex(n);
        return measure.getNthTimestampedMeasures(n);
    }

    private int ensureIndex(int upToIndex) {
        return Math.min(upToIndex, samples - 1);
    }

    protected OngoingPowerMeasure underlyingMeasure() {
        return measure;
    }
}
