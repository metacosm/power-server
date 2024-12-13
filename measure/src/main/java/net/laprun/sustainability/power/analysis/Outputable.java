package net.laprun.sustainability.power.analysis;

public interface Outputable {
    default String name() {
        return this.getClass().getSimpleName();
    }

    default String output() {
        return "";
    }
}
