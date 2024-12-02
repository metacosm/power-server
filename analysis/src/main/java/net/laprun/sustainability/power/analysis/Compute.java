package net.laprun.sustainability.power.analysis;

import org.apache.commons.math3.util.FastMath;

import net.laprun.sustainability.power.measure.PowerMeasure;

public enum Compute {
    ;

    public static double standardDeviation(PowerMeasure measure, int componentIndex) {
        return measure.getMeasuresFor(componentIndex).map(values -> {
            final var samples = values.length;
            if (samples <= 1) {
                return 0.0;
            }
            final double mean = average(measure, componentIndex);
            double geometricDeviationTotal = 0.0;
            for (double value : values) {
                double deviation = value - mean;
                geometricDeviationTotal += (deviation * deviation);
            }
            return FastMath.sqrt(geometricDeviationTotal / (samples - 1));
        }).orElse(0.0);
    }

    public static double average(PowerMeasure measure, int componentIndex) {
        return measure.getMeasuresFor(componentIndex).map(values -> {
            double sum = 0.0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }).orElse(0.0);
    }
}
