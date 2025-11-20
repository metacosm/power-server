package net.laprun.sustainability.power.sensors.linux.rapl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.PIDRegistry;
import net.laprun.sustainability.power.sensors.RegisteredPID;

class SingleMeasureMeasures implements Measures {
    private final Set<RegisteredPID> trackedPIDs = new HashSet<>();
    private SensorMeasure measure;
    private final PIDRegistry registry = new PIDRegistry();

    void singleMeasure(SensorMeasure sensorMeasure) {
        this.measure = sensorMeasure;
    }

    @Override
    public RegisteredPID register(long pid) {
        final var registeredPID = RegisteredPID.create(pid);
        trackedPIDs.add(registeredPID);
        registry.register(registeredPID);
        return registeredPID;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        trackedPIDs.remove(registeredPID);
        registry.unregister(registeredPID);
    }

    @Override
    public Set<RegisteredPID> trackedPIDs() {
        return trackedPIDs;
    }

    @Override
    public Set<String> trackedPIDsAsString() {
        return registry.pids();
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
}
