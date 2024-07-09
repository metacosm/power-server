package net.laprun.sustainability.power.measure;

interface MeasureStore {
    void recordComponentValue(int component, double value);

    void recordTotal(double value);

    double getMeasuredTotal();

    double getComponentStandardDeviation(int component);

    double getTotalStandardDeviation();

    double[] getComponentRawValues(int component);
}
