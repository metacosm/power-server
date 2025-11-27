package net.laprun.sustainability.power;

import java.util.Arrays;

/**
 * A power consumption measure as recorded by a sensor, recorded over a given period of time. The meaning of each component
 * measure is provided by the {@link SensorMetadata} information associated
 * with the sensor.
 */
public interface SensorMeasure {

    double MISSING_CPU_SHARE = -1.0;
    double MISSING_COMPONENT_VALUE = -1.0;
    long MISSING_TIME = -1;

    /**
     * Array recording the power consumption reported by each component of this sensor
     *
     * @return the values for each power component, as described in the {@link SensorMetadata} associated with the sensor
     */
    default double[] components() {
        return new double[] { MISSING_COMPONENT_VALUE };
    }

    /**
     * The start timestamp in milliseconds for this measure
     *
     * @return the start timestamp in milliseconds for this measure
     */
    default long startMs() {
        return MISSING_TIME;
    }

    /**
     * The end timestamp in milliseconds for this measure
     *
     * @return the end timestamp in milliseconds for this measure
     */
    default long endMs() {
        return MISSING_TIME;
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

    default double externalCPUShare() {
        return MISSING_CPU_SHARE;
    }

    /**
     * Represents an invalid or somehow missed measure.
     */
    SensorMeasure missing = new SensorMeasure() {
    };

    default String asString() {
        return getClass().getSimpleName() + '(' + startMs() + ',' + endMs() + " -> duration: " + durationMs() + ')'
                + Arrays.toString(components());
    }
}
