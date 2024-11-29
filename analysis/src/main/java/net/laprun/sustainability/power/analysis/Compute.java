package net.laprun.sustainability.power.analysis;

import org.apache.commons.math3.util.FastMath;

import net.laprun.sustainability.power.measure.PowerMeasure;

public enum Compute {
    ;

    public static double standardDeviation(PowerMeasure measure, int componentIndex) {
        return measure.getMeasuresFor(componentIndex).map(values -> {
            final var samples = measure.numberOfSamples();
            if (samples <= 1) {
                return 0.0;
            }
            final double mean = measure.averagesPerComponent()[componentIndex];
            double geometricDeviationTotal = 0.0;
            for (int index = 0; index < samples; index++) {
                double deviation = values[index] - mean;
                geometricDeviationTotal += (deviation * deviation);
            }
            return FastMath.sqrt(geometricDeviationTotal / (samples - 1));
        }).orElse(0.0);
    }
}
