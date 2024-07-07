package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import net.laprun.sustainability.power.SensorMetadata;

public interface PowerMeasure {
    private static double sumOfComponents(double[] recorded) {
        var componentSum = 0.0;
        for (double value : recorded) {
            componentSum += value;
        }
        return componentSum;
    }

    static double sumOfSelectedComponents(double[] recorded, int... indices) {
        if (indices == null || indices.length == 0) {
            return sumOfComponents(recorded);
        }
        var componentSum = 0.0;
        for (int index : indices) {
            componentSum += recorded[index];
        }
        return componentSum;
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

    default StdDev standardDeviations() {
        final var cardinality = metadata().componentCardinality();
        final var totalComponents = metadata().totalComponents();
        final DescriptiveStatistics[] perComponent = new DescriptiveStatistics[cardinality];
        for (int i = 0; i < perComponent.length; i++) {
            perComponent[i] = new DescriptiveStatistics();
        }
        final DescriptiveStatistics total = new DescriptiveStatistics();
        IntStream.range(0, cardinality).parallel()
                .forEach(component -> {
                    measures().stream().parallel().forEach(measure -> {
                        perComponent[component].addValue(measure[component]);
                        total.addValue(sumOfSelectedComponents(measure, totalComponents));
                    });
                });

        final var stdDevs = new double[cardinality];
        for (int i = 0; i < perComponent.length; i++) {
            stdDevs[i] = perComponent[i].getStandardDeviation();
        }
        return new StdDev(total.getStandardDeviation(), stdDevs);
    }

    List<double[]> measures();

    /**
     * Records the standard deviations for the aggregated energy comsumption value (as returned by {@link #total()}) and
     * per component
     *
     * @param aggregate
     * @param perComponent
     */
    record StdDev(double aggregate, double[] perComponent) {
    }
}
