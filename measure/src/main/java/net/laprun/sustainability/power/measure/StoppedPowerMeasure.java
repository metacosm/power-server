package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.DefaultProcessors;
import net.laprun.sustainability.power.analysis.Processors;

@SuppressWarnings("unused")
public class StoppedPowerMeasure implements PowerMeasure {
    private final OngoingPowerMeasure measure;
    private final int samples;
    private final Duration duration;
    private final double total;
    private final double min;
    private final double max;
    private Processors processors;

    public StoppedPowerMeasure(OngoingPowerMeasure powerMeasure) {
        this.measure = powerMeasure;
        this.duration = powerMeasure.duration();
        this.total = powerMeasure.total();
        this.min = powerMeasure.minMeasuredTotal();
        this.max = powerMeasure.maxMeasuredTotal();
        this.samples = powerMeasure.numberOfSamples();
        final var cardinality = metadata().componentCardinality();
        processors = powerMeasure.processors();
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

    @Override
    public Processors processors() {
        return processors;
    }

    @Override
    public void registerProcessorFor(int component, ComponentProcessor processor) {
        if (processor != null) {
            if (Processors.empty == processors) {
                processors = new DefaultProcessors(metadata().componentCardinality());
            }
            processors.registerProcessorFor(component, processor);
        }
    }

    @Override
    public void registerTotalProcessor(ComponentProcessor processor) {
        if (processor != null) {
            if (Processors.empty == processors) {
                processors = new DefaultProcessors(metadata().componentCardinality());
            }
            processors.registerTotalProcessor(processor);
        }
    }
}
