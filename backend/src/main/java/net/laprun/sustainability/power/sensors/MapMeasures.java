package net.laprun.sustainability.power.sensors;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.laprun.sustainability.power.SensorMeasure;

public class MapMeasures implements Measures {
    private final ConcurrentMap<RegisteredPID, SensorMeasure> measures = new ConcurrentHashMap<>();

    @Override
    public Measures record(RegisteredPID pid, SensorMeasure sensorMeasure) {
        measures.put(pid, sensorMeasure);
        return this;
    }

    @Override
    public SensorMeasure getOrDefault(RegisteredPID pid) {
        return measures.getOrDefault(pid, SensorMeasure.missing);
    }

    @Override
    public String toString() {
        return measures.toString();
    }

    @Override
    public void clear() {
        measures.clear();
    }
}
