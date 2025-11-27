package net.laprun.sustainability.power.measures;

import net.laprun.sustainability.power.SensorMeasure;

/**
 * A {@link SensorMeasure} without explicit duration so assumed to last for the whole interval defined by the difference between
 * end and start times.
 *
 * @param components an array recording the power consumption reported by each component of this sensor
 * @param startMs the start timestamp in milliseconds for this measure
 * @param endMs the end timestamp in milliseconds for this measure
 */
public record NoDurationSensorMeasure(double[] components, long startMs, long endMs) implements SensorMeasure {
    @Override
    public String toString() {
        return asString();
    }
}
