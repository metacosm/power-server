package net.laprun.sustainability.power.measure;

import java.time.Duration;

public class StoppedPowerMeasure extends AbstractPowerMeasure {
    private final Duration duration;
    private final double total;
    private final double min;
    private final double max;
    private final double[] averages;

    public StoppedPowerMeasure(PowerMeasure powerMeasure) {
        super(powerMeasure.metadata(), powerMeasure.measures());
        this.duration = powerMeasure.duration();
        this.total = powerMeasure.total();
        this.min = powerMeasure.minMeasuredTotal();
        this.max = powerMeasure.maxMeasuredTotal();
        this.averages = powerMeasure.averagesPerComponent();
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
    public double[] averagesPerComponent() {
        return averages;
    }
}
