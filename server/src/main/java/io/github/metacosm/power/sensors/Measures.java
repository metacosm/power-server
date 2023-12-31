package io.github.metacosm.power.sensors;

import io.github.metacosm.power.SensorMeasure;

import java.util.Set;

public interface Measures {
    SensorMeasure missing = new SensorMeasure(new double[]{-1.0}, -1);
    RegisteredPID register(long pid);

    void unregister(RegisteredPID registeredPID);

    Set<RegisteredPID> trackedPIDs();

    int numberOfTrackerPIDs();

    void record(RegisteredPID pid, SensorMeasure sensorMeasure);

    SensorMeasure getOrDefault(RegisteredPID pid);
}
