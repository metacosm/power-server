package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.MeasureProcessor;
import net.laprun.sustainability.power.analysis.Processors;

public interface PowerMeasure {

    @SuppressWarnings("unused")
    static String asString(PowerMeasure measure) {
        final var durationInSeconds = measure.duration().getSeconds();
        final var samples = measure.numberOfSamples();
        return String.format("%ds, %s samples\n---\n%s", durationInSeconds, samples,
                measure.processors().output(measure.metadata()));
    }

    int numberOfSamples();

    Duration duration();

    SensorMetadata metadata();

    Optional<double[]> getMeasuresFor(int component);

    TimestampedMeasures getNthTimestampedMeasures(int n);

    Stream<TimestampedValue> streamTimestampedMeasuresFor(int component, int upToIndex);

    DoubleStream streamMeasuresFor(int component, int upToIndex);

    record TimestampedValue(long timestamp, double value) {
    }

    record TimestampedMeasures(long timestamp, double[] measures) {
    }

    Processors processors();

    void registerProcessorFor(int component, ComponentProcessor processor);

    void registerMeasureProcessor(MeasureProcessor processor);
}
