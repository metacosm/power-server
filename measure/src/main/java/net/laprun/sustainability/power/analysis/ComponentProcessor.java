package net.laprun.sustainability.power.analysis;

public interface ComponentProcessor extends Outputable {
    default void recordComponentValue(double value, long timestamp) {
    }
}
