package net.laprun.sustainability.power.measure;

public interface ComponentMeasure {
    void recordComponentValue(double value);

    double[] getComponentRawValues();

    interface Factory<T extends ComponentMeasure> {
        T create();
    }
}
