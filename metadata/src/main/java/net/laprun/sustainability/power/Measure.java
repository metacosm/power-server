package net.laprun.sustainability.power;

public record Measure(double value, SensorUnit unit) {
    @Override
    public String toString() {
        return value + unit.toString();
    }
}
