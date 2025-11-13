package net.laprun.sustainability.power.sensors;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import net.laprun.sustainability.power.SensorMeasure;

public class MapMeasures implements Measures {
    private final ConcurrentMap<RegisteredPID, SensorMeasure> measures = new ConcurrentHashMap<>();
    private long lastMeasuredUpdateStartEpoch;
    private final PIDRegistry registry = new PIDRegistry();

    @Override
    public RegisteredPID register(long pid) {
        final var key = RegisteredPID.create(pid);
        measures.put(key, SensorMeasure.missing);
        registry.register(key);
        return key;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.remove(registeredPID);
        registry.unregister(registeredPID);
    }

    @Override
    public Set<RegisteredPID> trackedPIDs() {
        return measures.keySet();
    }

    @Override
    public Set<String> trackedPIDsAsString() {
        return registry.pids();
    }

    @Override
    public int numberOfTrackedPIDs() {
        return measures.size();
    }

    @Override
    public void record(RegisteredPID pid, SensorMeasure sensorMeasure) {
        lastMeasuredUpdateStartEpoch = sensorMeasure.endMs();
        measures.put(pid, sensorMeasure);
    }

    @Override
    public SensorMeasure getOrDefault(RegisteredPID pid) {
        return measures.getOrDefault(pid, SensorMeasure.missing);
    }

    @Override
    public long lastMeasuredUpdateEndEpoch() {
        return lastMeasuredUpdateStartEpoch;
    }

    @Override
    public void forEach(Consumer<? super SensorMeasure> consumer) {
        measures.keySet().forEach(pid -> consumer.accept(getOrDefault(pid)));
    }
}
