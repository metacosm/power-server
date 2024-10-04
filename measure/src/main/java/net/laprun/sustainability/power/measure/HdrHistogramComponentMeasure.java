package net.laprun.sustainability.power.measure;

import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.IntCountsHistogram;

public class HdrHistogramComponentMeasure implements ComponentMeasure {
    private static final int HIGHEST_TRACKABLE_VALUE = 1_000_000;
    private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 4;
    private static final int CONVERSION_FACTOR = 1000;
    private final IntCountsHistogram histogram;

    public HdrHistogramComponentMeasure() {
        histogram = new IntCountsHistogram(HIGHEST_TRACKABLE_VALUE, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
    }

    @Override
    public void recordComponentValue(double value) {
        histogram.recordValue((long) (CONVERSION_FACTOR * value));
    }

    @Override
    public double[] getComponentRawValues() {
        final var totalCount = histogram.getTotalCount();
        if (totalCount == 0) {
            return new double[0];
        }

        final var result = new double[(int) totalCount];
        int index = 0;
        for (HistogramIterationValue value : histogram.recordedValues()) {
            result[index++] = (double) value.getValueIteratedTo() / CONVERSION_FACTOR;
        }
        return result;
    }
}
