package io.github.metacosm.power.sensors.linux.rapl;

import io.github.metacosm.power.SensorMeasure;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.RegisteredPID;

import java.util.HashSet;
import java.util.Set;

public class SingleMeasureMeasures implements Measures {
    private final Set<RegisteredPID> trackedPIDs = new HashSet<>();
    private SensorMeasure measure;
    void singleMeasure(SensorMeasure sensorMeasure) {
        this.measure = sensorMeasure;
    }

    @Override
    public RegisteredPID register(long pid) {
        final var registeredPID = new RegisteredPID(pid);
        trackedPIDs.add(registeredPID);
        return registeredPID;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        trackedPIDs.remove(registeredPID);
    }

    @Override
    public Set<RegisteredPID> trackedPIDs() {
        return trackedPIDs;
    }

    @Override
    public int numberOfTrackerPIDs() {
        return trackedPIDs.size();
    }

    @Override
    public void record(RegisteredPID pid, SensorMeasure sensorMeasure) {
       throw new UnsupportedOperationException("Shouldn't be needed");
    }

    @Override
    public SensorMeasure getOrDefault(RegisteredPID pid) {
        return trackedPIDs.contains(pid) && measure != null ? measure : Measures.missing;
    }
}
