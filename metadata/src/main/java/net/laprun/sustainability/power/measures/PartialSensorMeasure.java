package net.laprun.sustainability.power.measures;

import net.laprun.sustainability.power.SensorMeasure;

/**
 * A {@link SensorMeasure} with an explicit duration, so not covering the entirety of the recorded interval. Useful for sensors
 * not recording for the whole duration of the measure.
 *
 * @param components an array recording the power consumption reported by each component of this sensor
 * @param startMs the start timestamp in milliseconds for this measure
 * @param endMs the end timestamp in milliseconds for this measure
 * @param durationMs the duration of the effective measure done by the sensor
 */
public record PartialSensorMeasure(double[] components, long startMs, long endMs,
        long durationMs) implements SensorMeasure {
}
