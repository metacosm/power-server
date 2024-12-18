package net.laprun.sustainability.power.analysis;

public interface Outputable {
    default String name() {
        final var simpleName = this.getClass().getSimpleName();
        return !simpleName.isBlank() ? simpleName : this.getClass().getName();
    }

    default String output() {
        return "";
    }
}
