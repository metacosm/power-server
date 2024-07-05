package net.laprun.sustainability.power.measure;

import java.time.Duration;
import java.util.ArrayList;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasure extends AbstractPowerMeasure {
    private final long startedAt;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private final double[] totals;
    private final double[] averages;

    public OngoingPowerMeasure(SensorMetadata sensorMetadata, long duration, long frequency) {
        super(sensorMetadata, new ArrayList<>((int) (duration / frequency)));
        startedAt = System.currentTimeMillis();
        final var numComponents = metadata().componentCardinality();
        totals = new double[numComponents];
        averages = new double[numComponents];
    }

    public void recordMeasure(double[] components) {
        final var recorded = new double[components.length];
        System.arraycopy(components, 0, recorded, 0, components.length);
        final var previousSize = numberOfSamples();
        measures().add(recorded);

        for (int i = 0; i < recorded.length; i++) {
            totals[i] += recorded[i];
            averages[i] = averages[i] == 0 ? recorded[i]
                    : (previousSize * averages[i] + recorded[i]) / numberOfSamples();
        }

        // record min / max totals
        final var recordedTotal = PowerMeasure.sumOfComponents(recorded);
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }

    @Override
    public double total() {
        return PowerMeasure.sumOfComponents(totals);
    }

    public Duration duration() {
        return Duration.ofMillis(System.currentTimeMillis() - startedAt);
    }

    @Override
    public double minMeasuredTotal() {
        return minTotal;
    }

    @Override
    public double maxMeasuredTotal() {
        return maxTotal;
    }

    @Override
    public double[] averagesPerComponent() {
        return averages;
    }
}
