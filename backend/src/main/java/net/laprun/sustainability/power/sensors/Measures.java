package net.laprun.sustainability.power.sensors;

import net.laprun.sustainability.power.SensorMeasure;

/**
 * A representation of ongoing {@link PowerSensor} measures.
 */
public interface Measures {

    /**
     * Records the specified measure and associates it to the specified tracked process, normally called once per tick
     *
     * @param pid the {@link RegisteredPID} representing the tracked process with which the recorded measure needs to be
     *        associated
     * @param sensorMeasure the {@link SensorMeasure} to be recorded
     */
    Measures record(RegisteredPID pid, SensorMeasure sensorMeasure);

    /**
     * Retrieves the last recorded {@link SensorMeasure} associated with the specified {@link RegisteredPID}
     *
     * @param pid the tracked process identifier which measure we want to retrieve
     * @return the last recorded {@link SensorMeasure} associated with the specified process or {@link SensorMeasure#missing} if
     *         it cannot be
     *         retrieved for any reason
     */
    default SensorMeasure getOrDefault(RegisteredPID pid) {
        return SensorMeasure.missing;
    }

    @SuppressWarnings("unused")
    default SensorMeasure getSystemTotal() {
        return getOrDefault(RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID);
    }

    void clear();
}
