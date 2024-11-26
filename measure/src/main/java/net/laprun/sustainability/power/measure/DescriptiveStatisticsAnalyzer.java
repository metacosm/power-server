package net.laprun.sustainability.power.measure;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@SuppressWarnings("unused")
public class DescriptiveStatisticsAnalyzer implements Analyzer {
    private final DescriptiveStatistics statistics;

    public DescriptiveStatisticsAnalyzer() {
        statistics = new DescriptiveStatistics();
    }

    @Override
    public void recordComponentValue(double value, long timestamp) {
        statistics.addValue(value);
    }
}
