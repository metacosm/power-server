package net.laprun.sustainability.power.analysis;

import org.apache.commons.math3.util.FastMath;

import net.laprun.sustainability.power.measure.PowerMeasure;

public enum Compute {
    ;

    public static double standardDeviation(PowerMeasure measure, int componentIndex) {
        final var measures = measure.getMeasuresFor(componentIndex).toArray();
        final var samples = measures.length;
        if (samples <= 1) {
            return 0.0;
        }
        final double mean = average(measures);
        double geometricDeviationTotal = 0.0;
        for (double value : measures) {
            double deviation = value - mean;
            geometricDeviationTotal += (deviation * deviation);
        }
        return FastMath.sqrt(geometricDeviationTotal / (samples - 1));
    }

    public static double average(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public static double average(PowerMeasure measure, int componentIndex) {
        return measure.getMeasuresFor(componentIndex).average().orElse(0.0);
    }
}
