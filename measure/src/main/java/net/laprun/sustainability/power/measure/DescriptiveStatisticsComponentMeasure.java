package net.laprun.sustainability.power.measure;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class DescriptiveStatisticsComponentMeasure implements ComponentMeasure {
    private final DescriptiveStatistics statistics;

    public DescriptiveStatisticsComponentMeasure(int initialWindow) {
        statistics = new DescriptiveStatistics(initialWindow);
    }

    @Override
    public void recordComponentValue(double value) {
        statistics.addValue(value);
    }

    @Override
    public double[] getComponentRawValues() {
        return statistics.getValues();
    }

    public static Factory<DescriptiveStatisticsComponentMeasure> factory(int initialWindow) {
        return () -> new DescriptiveStatisticsComponentMeasure(initialWindow);
    }
}
