package net.laprun.sustainability.power.sensors;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.laprun.sustainability.power.SensorMeasure;

public class MapMeasures implements Measures {
    private final ConcurrentMap<RegisteredPID, SensorMeasure> measures = new ConcurrentHashMap<>();

    @Override
    public RegisteredPID register(long pid) {
        final var key = new RegisteredPID(pid);
        measures.put(key, SensorMeasure.missing);
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
        return measures.getOrDefault(pid, SensorMeasure.missing);
    }
}
