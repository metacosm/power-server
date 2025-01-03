package net.laprun.sustainability.power.analysis;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@SuppressWarnings("unused")
public class DescriptiveStatisticsComponentProcessor implements ComponentProcessor {
    private final DescriptiveStatistics statistics;

    public DescriptiveStatisticsComponentProcessor() {
        statistics = new DescriptiveStatistics();
    }

    public DescriptiveStatistics statistics() {
        return statistics;
    }

    @Override
    public void recordComponentValue(double value, long timestamp) {
        statistics.addValue(value);
    }
}
