package net.laprun.sustainability.power.analysis;

public interface MeasureProcessor extends Outputable {
    default void recordMeasure(double[] measure, long timestamp) {
    }
}
