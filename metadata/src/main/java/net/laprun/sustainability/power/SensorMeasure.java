package net.laprun.sustainability.power;

/**
 * A power consumption measure as recorded by a sensor, recorded over a given period of time, with an ordering information
 * provided by a tick. The meaning of each component measure is provided by the {@link SensorMetadata} information associated
 * with the sensor.
 *
 * @param components an array recording the power consumption reported by each component of this sensor
 * @param tick the ordinal tick associated with this measure
 */
public record SensorMeasure(double[] components, long tick, long timestamp, long duration) {
    /**
     * Represents an invalid or somehow missed measure.
     */
    public static final SensorMeasure missing = new SensorMeasure(new double[] { -1.0 }, -1, -1, -1);
}
