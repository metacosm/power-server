package net.laprun.sustainability.power;

/**
 * A power consumption measure as recorded by a sensor, recorded over a given period of time. The meaning of each component
 * measure is provided by the {@link SensorMetadata} information associated
 * with the sensor.
 */
public interface SensorMeasure {

    /**
     * Array recording the power consumption reported by each component of this sensor
     *
     * @return the values for each power component, as described in the {@link SensorMetadata} associated with the sensor
     */
    default double[] components() {
        return new double[] { -1.0 };
    }

    /**
     * The start timestamp in milliseconds for this measure
     *
     * @return the start timestamp in milliseconds for this measure
     */
    default long startMs() {
        return -1;
    }

    /**
     * The end timestamp in milliseconds for this measure
     *
     * @return the end timestamp in milliseconds for this measure
     */
    default long endMs() {
        return -1;
    }

    /**
     * The measure duration in milliseconds. Note that while this could be determined from the start and end
     * timestamps of the measure, some sensors don't provide values for the whole duration of the measure. In that case, if
     * the sensor provides this information, it is provided using this parameter. If the sensor recorded for the whole
     * duration or doesn't provide the recorded measure, this value will be equal to the difference between start and end
     * timestamps.
     *
     * @return the measure duration in milliseconds
     */
    default long durationMs() {
        return endMs() - startMs();
    }

    /**
     * Whether this measure didn't cover the totatility of the recorded time
     *
     * @return {@code true} if the components were only recorded for part of the interval defined by the difference between end
     *         and start times, {@code false} otherwise
     */
    default boolean isPartial() {
        return false;
    }

    /**
     * Represents an invalid or somehow missed measure.
     */
    SensorMeasure missing = new SensorMeasure() {
    };
}
