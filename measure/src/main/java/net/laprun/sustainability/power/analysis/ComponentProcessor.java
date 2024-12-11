package net.laprun.sustainability.power.analysis;

public interface ComponentProcessor {
    default void recordComponentValue(double value, long timestamp) {
    }

    default String name() {
        return this.getClass().getSimpleName();
    }

    default String output() {
        return "";
    }
}
