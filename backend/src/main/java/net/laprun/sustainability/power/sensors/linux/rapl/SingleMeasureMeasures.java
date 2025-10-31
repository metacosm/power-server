package net.laprun.sustainability.power.sensors.linux.rapl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.RegisteredPID;

class SingleMeasureMeasures implements Measures {
    private final Set<RegisteredPID> trackedPIDs = new HashSet<>();
    private final Set<String> pids = new HashSet<>();
    private SensorMeasure measure;

    void singleMeasure(SensorMeasure sensorMeasure) {
        this.measure = sensorMeasure;
    }

    @Override
    public RegisteredPID register(long pid) {
        final var registeredPID = new RegisteredPID(pid);
        trackedPIDs.add(registeredPID);
        pids.add(registeredPID.pidAsString());
        return registeredPID;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        trackedPIDs.remove(registeredPID);
        pids.remove(registeredPID.pidAsString());
    }

    @Override
    public Set<RegisteredPID> trackedPIDs() {
        return trackedPIDs;
    }

    public Set<String> pids() {
        return pids;
    }

    @Override
    public int numberOfTrackedPIDs() {
        return trackedPIDs.size();
    }

    @Override
    public void record(RegisteredPID pid, SensorMeasure sensorMeasure) {
        throw new UnsupportedOperationException("Shouldn't be needed");
    }

    @Override
    public SensorMeasure getOrDefault(RegisteredPID pid) {
        return trackedPIDs.contains(pid) && measure != null ? measure : SensorMeasure.missing;
    }

    @Override
    public void forEach(Consumer<? super SensorMeasure> consumer) {
        throw new UnsupportedOperationException("todo: not implemented yet");
    }

    @Override
    public long lastMeasuredUpdateEndEpoch() {
        return -1;
    }
}
