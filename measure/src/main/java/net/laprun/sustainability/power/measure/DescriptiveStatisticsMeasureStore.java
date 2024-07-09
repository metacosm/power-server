package net.laprun.sustainability.power.measure;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

class DescriptiveStatisticsMeasureStore implements MeasureStore {
    private final DescriptiveStatistics[] measures;
    private final DescriptiveStatistics total;

    public DescriptiveStatisticsMeasureStore(int componentsNumber, int initialWindow) {
        total = new DescriptiveStatistics(initialWindow);
        this.measures = new DescriptiveStatistics[componentsNumber];
        for (int i = 0; i < measures.length; i++) {
            measures[i] = new DescriptiveStatistics(initialWindow);
        }
    }

    @Override
    public void recordComponentValue(int component, double value) {
        measures[component].addValue(value);
    }

    @Override
    public void recordTotal(double value) {
        total.addValue(value);
    }

    @Override
    public double getMeasuredTotal() {
        return total.getSum();
    }

    @Override
    public double getComponentStandardDeviation(int component) {
        return measures[component].getStandardDeviation();
    }

    @Override
    public double getTotalStandardDeviation() {
        return total.getStandardDeviation();
    }

    @Override
    public double[] getComponentRawValues(int component) {
        return measures[component].getValues();
    }
}
