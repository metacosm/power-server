package net.laprun.sustainability.power.measure;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

class DescriptiveStatisticsMeasureStore implements MeasureStore {
    private final int totalIndex;
    private final DescriptiveStatistics[] measures;

    public DescriptiveStatisticsMeasureStore(int componentsNumber, int initialWindow) {
        this.measures = new DescriptiveStatistics[componentsNumber + 1];
        totalIndex = componentsNumber;
        for (int i = 0; i < measures.length; i++) {
            measures[i] = new DescriptiveStatistics(initialWindow);
        }
    }

    @Override
    public void recordComponentValue(int component, double value) {
        getMeasure(component).addValue(value);
    }

    private DescriptiveStatistics getMeasure(int component) {
        return measures[component];
    }

    private DescriptiveStatistics getTotalMeasure() {
        return getMeasure(totalIndex);
    }

    @Override
    public void recordTotal(double value) {
        getTotalMeasure().addValue(value);
    }

    @Override
    public double getMeasuredTotal() {
        return getTotalMeasure().getSum();
    }

    @Override
    public double getComponentStandardDeviation(int component) {
        return getMeasure(component).getStandardDeviation();
    }

    @Override
    public double getTotalStandardDeviation() {
        return getTotalMeasure().getStandardDeviation();
    }

    @Override
    public double[] getComponentRawValues(int component) {
        return getMeasure(component).getValues();
    }
}
