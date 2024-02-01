package io.github.metacosm.power.sensors.test;

import java.util.Map;

import io.github.metacosm.power.SensorMeasure;
import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.MapMeasures;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;

public class TestPowerSensor implements PowerSensor {
    public static final String CPU = "cpu";
    public static final SensorMetadata DEFAULT = new SensorMetadata(
            Map.of(CPU, new SensorMetadata.ComponentMetadata(CPU, 0, "CPU", true, "mW")),
            "Test PowerSensor returning random values for a single 'cpu' component");
    private final SensorMetadata metadata;
    private boolean started;
    private final Measures measures = new MapMeasures();

    public TestPowerSensor() {
        metadata = DEFAULT;
    }

    public TestPowerSensor(SensorMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public SensorMetadata metadata() {
        return metadata;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start(long samplingFrequencyInMillis) throws Exception {
        if (!started) {
            started = true;
        }
    }

    @Override
    public RegisteredPID register(long pid) {
        return measures.register(pid);
    }

    @Override
    public Measures update(Long tick) {
        measures.trackedPIDs().forEach(pid -> measures.record(pid, new SensorMeasure(new double[] { Math.random() }, tick)));
        return measures;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
    }
}
