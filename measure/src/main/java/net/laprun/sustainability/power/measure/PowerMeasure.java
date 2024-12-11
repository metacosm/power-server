package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.Processors;

public interface PowerMeasure {

    @SuppressWarnings("unused")
    static String asString(PowerMeasure measure) {
        final var durationInSeconds = measure.duration().getSeconds();
        final var samples = measure.numberOfSamples();
        final var measuredMilliWatts = measure.total();
        return String.format("%s [min: %.3f, max: %.3f] (%ds, %s samples)\n---\n%s",
                readableWithUnit(measuredMilliWatts),
                measure.minMeasuredTotal(), measure.maxMeasuredTotal(), durationInSeconds, samples,
                measure.processors().output(measure.metadata()));
    }

    static String readableWithUnit(double milliWatts) {
        String unit = milliWatts >= 1000 ? "W" : "mW";
        double power = milliWatts >= 1000 ? milliWatts / 1000 : milliWatts;
        return String.format("%.3f%s", power, unit);
    }

    int numberOfSamples();

    Duration duration();

    double total();

    SensorMetadata metadata();

    double minMeasuredTotal();

    double maxMeasuredTotal();

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
}
