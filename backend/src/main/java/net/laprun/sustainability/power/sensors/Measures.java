package net.laprun.sustainability.power.sensors;

import java.util.Set;

import net.laprun.sustainability.power.SensorMeasure;

/**
 * A representation of ongoing {@link PowerSensor} measures.
 */
public interface Measures {

    /**
     * Tracks the provided process identifier (pid) in the measures. For sensors that only provide system-wide measures, this
     * probably won't be doing much more than track which processes are of interest to clients of the sensor.
     *
     * @param pid the process identifier which power consumption is supposed to be tracked
     * @return a {@link RegisteredPID} recording the tracking of the specified pid by the sensor
     */
    RegisteredPID register(long pid);

    /**
     * Unregisters the specified {@link RegisteredPID} thus signaling that clients are not interested in tracking the
     * consumption of the associated process anymore
     *
     * @param registeredPID the {@link RegisteredPID} that was returned when the process we want to stop tracking was first
     *        registered
     */
    void unregister(RegisteredPID registeredPID);

    /**
     * Retrieves the set of tracked process identifiers
     *
     * @return the set of tracked process identifiers
     */
    Set<RegisteredPID> trackedPIDs();

    /**
     * Retrieves the number of tracked processes
     *
     * @return the number of tracked processes
     */
    int numberOfTrackedPIDs();

    /**
     * Records the specified measure and associates it to the specified tracked process, normally called once per tick
     *
     * @param pid the {@link RegisteredPID} representing the tracked process with which the recorded measure needs to be
     *        associated
     * @param sensorMeasure the {@link SensorMeasure} to be recorded
     */
    void record(RegisteredPID pid, SensorMeasure sensorMeasure);

    /**
     * Retrieves the last recorded {@link SensorMeasure} associated with the specified {@link RegisteredPID}
     *
     * @param pid the tracked process identifier which measure we want to retrieve
     * @return the last recorded {@link SensorMeasure} associated with the specified process or {@link SensorMeasure#missing} if
     *         it cannot be
     *         retrieved for any reason
     */
    SensorMeasure getOrDefault(RegisteredPID pid);

    /**
     * Returns the last measured end epoch of an update, if it exists.
     *
     * @return the last measured end epoch of an update, or {@code -1} if the measure didn't provide that information
     */
    long lastMeasuredUpdateEndEpoch();
}
