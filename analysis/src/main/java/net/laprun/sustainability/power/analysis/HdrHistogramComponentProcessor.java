package net.laprun.sustainability.power.analysis;

import org.HdrHistogram.IntCountsHistogram;

@SuppressWarnings("unused")
public class HdrHistogramComponentProcessor implements ComponentProcessor {
    private static final int HIGHEST_TRACKABLE_VALUE = 1_000_000;
    private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 4;
    private static final int CONVERSION_FACTOR = 1000;
    private final IntCountsHistogram histogram;

    public HdrHistogramComponentProcessor() {
        histogram = new IntCountsHistogram(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    }

    @Override
    public void recordComponentValue(double value, long timestamp) {
        histogram.recordValue((long) (CONVERSION_FACTOR * value));
    }
}
