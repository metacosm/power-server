package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import net.laprun.sustainability.power.SensorMetadata;

public interface PowerMeasure {
    int numberOfSamples();

    Duration duration();

    default double average() {
        return total() / numberOfSamples();
    }

    double total();

    SensorMetadata metadata();

    double[] averagesPerComponent();

    double minMeasuredTotal();

    double maxMeasuredTotal();

    static double sumOfComponents(double[] recorded) {
        var componentSum = 0.0;
        for (double value : recorded) {
            componentSum += value;
        }
        return componentSum;
    }

    default StdDev standardDeviations() {
        final var cardinality = metadata().componentCardinality();
        final var stdDevs = new double[cardinality];
        final var aggregate = new double[1];
        final var samples = numberOfSamples() - 1; // unbiased so we remove one sample
        final var sqrdAverages = Arrays.stream(averagesPerComponent()).map(m -> m * m).toArray();
        final var sqrdAverage = average() * average();
        // need to compute the average of variances then square root that to get the "aggregate" standard deviation,
        // see: https://stats.stackexchange.com/a/26647
        // "vectorize" computation of variances: compute the variance for each component in parallel
        IntStream.range(0, cardinality).parallel()
                // compute variances for each component of the measure
                .forEach(component -> {
                    final var sumOfSquares = measures().stream().parallel().peek(m -> {
                        // compute the std dev for total measure
                        final var total = sumOfComponents(m);
                        aggregate[0] += total * total;
                    }).mapToDouble(m -> m[component] * m[component]).sum();
                    stdDevs[component] = stdDev(sumOfSquares, sqrdAverages[component], samples);
                    aggregate[0] = stdDev(aggregate[0], sqrdAverage, samples);
                });
        return new StdDev(aggregate[0], stdDevs);
    }

    private static double stdDev(double sumOfSquares, double squaredAvg, int samples) {
        return Math.sqrt((sumOfSquares / samples) - (((samples + 1) * squaredAvg) / samples));
    }

    /**
     * Records the standard deviations for the aggregated energy comsumption value (as returned by {@link #total()}) and
     * per component
     *
     * @param aggregate
     * @param perComponent
     */
    record StdDev(double aggregate, double[] perComponent) {
    }

    static String asString(PowerMeasure measure) {
        final var durationInSeconds = measure.duration().getSeconds();
        final var samples = measure.numberOfSamples();
        final var measuredMilliWatts = measure.total();
        final var stdDevs = measure.standardDeviations();
        return String.format("%s / avg: %s / std dev: %.3f [min: %.3f, max: %.3f] (%ds, %s samples)",
                readableWithUnit(measuredMilliWatts), readableWithUnit(measure.average()), stdDevs.aggregate,
                measure.minMeasuredTotal(), measure.maxMeasuredTotal(), durationInSeconds, samples);
    }

    static String readableWithUnit(double milliWatts) {
        String unit = milliWatts >= 1000 ? "W" : "mW";
        double power = milliWatts >= 1000 ? milliWatts / 1000 : milliWatts;
        return String.format("%.3f%s", power, unit);
    }

    List<double[]> measures();
}
