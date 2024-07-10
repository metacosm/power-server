package net.laprun.sustainability.power.measure;

import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.IntCountsHistogram;

public class HdrHistogramMeasureStore implements MeasureStore {
    private static final int HIGHEST_TRACKABLE_VALUE = 1_000_000;
    private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 4;
    private static final int CONVERSION_FACTOR = 1000;
    private final IntCountsHistogram[] measures;
    private final int totalIndex;
    private double accumulatedTotal;

    public HdrHistogramMeasureStore(int componentsNumber, int initialWindow) {
        totalIndex = componentsNumber;
        measures = new IntCountsHistogram[componentsNumber + 1];
        for (int i = 0; i < measures.length; i++) {
            measures[i] = new IntCountsHistogram(HIGHEST_TRACKABLE_VALUE,
                    NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
        }
    }

    private IntCountsHistogram getMeasure(int component) {
        return measures[component];
    }

    private IntCountsHistogram getTotalMeasure() {
        return getMeasure(totalIndex);
    }

    @Override
    public void recordComponentValue(int component, double value) {
        getMeasure(component).recordValue((long) (CONVERSION_FACTOR * value));
    }

    @Override
    public void recordTotal(double value) {
        getTotalMeasure().recordValue((long) (CONVERSION_FACTOR * value));
        accumulatedTotal += value;
    }

    @Override
    public double getMeasuredTotal() {
        return accumulatedTotal;
    }

    @Override
    public double getComponentStandardDeviation(int component) {
        return getMeasure(component).getStdDeviation() / CONVERSION_FACTOR;
    }

    @Override
    public double getTotalStandardDeviation() {
        // not unbiased so tests will fail
        return getTotalMeasure().getStdDeviation();
    }

    @Override
    public double[] getComponentRawValues(int component) {
        final var measure = getMeasure(component);
        final var totalCount = measure.getTotalCount();
        final var result = new double[(int) totalCount];
        int index = 0;
        for (HistogramIterationValue value : measure.recordedValues()) {
            result[index++] = (double) value.getValueIteratedTo() / CONVERSION_FACTOR;
        }
        return result;
    }
}
