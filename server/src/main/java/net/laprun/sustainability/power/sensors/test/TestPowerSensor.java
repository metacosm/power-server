package net.laprun.sustainability.power.sensors.test;

import java.util.Map;

import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.MapMeasures;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.RegisteredPID;

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
