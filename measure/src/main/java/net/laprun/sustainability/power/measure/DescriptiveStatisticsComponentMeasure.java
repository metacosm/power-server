package net.laprun.sustainability.power.measure;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class DescriptiveStatisticsComponentMeasure implements ComponentMeasure {
    private final DescriptiveStatistics statistics;

    public DescriptiveStatisticsComponentMeasure() {
        statistics = new DescriptiveStatistics();
    }

    @Override
    public void recordComponentValue(double value) {
        statistics.addValue(value);
    }

    @Override
    public double[] getComponentValues() {
        return statistics.getValues();
    }
}
