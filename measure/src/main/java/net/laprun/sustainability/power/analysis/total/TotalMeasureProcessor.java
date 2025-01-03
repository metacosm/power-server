package net.laprun.sustainability.power.analysis.total;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.MeasureProcessor;

public class TotalMeasureProcessor implements MeasureProcessor {
    private final Totaler totaler;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private double accumulatedTotal;

    public TotalMeasureProcessor(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        this.totaler = new Totaler(metadata, expectedResultUnit, totalComponentIndices);
        totaler.validate();
    }

    public double total() {
        return accumulatedTotal;
    }

    public double minMeasuredTotal() {
        return minTotal == Double.MAX_VALUE ? 0.0 : minTotal;
    }

    public double maxMeasuredTotal() {
        return maxTotal;
    }

    @Override
    public String name() {
        return totaler.name();
    }

    @Override
    public String output() {
        final var symbol = totaler.expectedResultUnit().symbol();
        return String.format("%.2f%s (min: %.2f / max: %.2f)", total(), symbol, minMeasuredTotal(), maxMeasuredTotal());
    }

    @Override
    public void recordMeasure(double[] measure, long timestamp) {
        final double recordedTotal = totaler.computeTotalFrom(measure);
        accumulatedTotal += recordedTotal;
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }
}
