package net.laprun.sustainability.power.measure;

public interface Analyzer {
    void recordComponentValue(double value, long timestamp);
}
