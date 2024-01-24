package io.github.metacosm.power.sensors.macos.powermetrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.metacosm.power.SensorMeasure;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.RegisteredPID;

class MapMeasures implements Measures {
    private final ConcurrentMap<RegisteredPID, SensorMeasure> measures = new ConcurrentHashMap<>();

    @Override
    public RegisteredPID register(long pid) {
        final var key = new RegisteredPID(pid);
        measures.put(key, missing);
        return key;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.remove(registeredPID);
    }

    @Override
    public Set<RegisteredPID> trackedPIDs() {
        return measures.keySet();
    }

    @Override
    public int numberOfTrackerPIDs() {
        return measures.size();
    }

    @Override
    public void record(RegisteredPID pid, SensorMeasure sensorMeasure) {
        measures.put(pid, sensorMeasure);
    }

    @Override
    public SensorMeasure getOrDefault(RegisteredPID pid) {
        return measures.getOrDefault(pid, missing);
    }
}
