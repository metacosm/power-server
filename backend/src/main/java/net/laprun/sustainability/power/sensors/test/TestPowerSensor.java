package net.laprun.sustainability.power.sensors.test;

import static net.laprun.sustainability.power.SensorUnit.mW;

import java.util.List;

import io.smallrye.mutiny.Multi;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.measures.NoDurationSensorMeasure;
import net.laprun.sustainability.power.sensors.AbstractPowerSensor;
import net.laprun.sustainability.power.sensors.Measures;

@SuppressWarnings("unused")
public class TestPowerSensor extends AbstractPowerSensor<Void> {
    public static final String CPU = "cpu";
    public static final SensorMetadata DEFAULT = new SensorMetadata(
            List.of(new SensorMetadata.ComponentMetadata(CPU, 0, "CPU", true, mW)),
            "Test PowerSensor returning random values for a single 'cpu' component");
    private final SensorMetadata metadata;

    public TestPowerSensor() {
        this(DEFAULT);
    }

    public TestPowerSensor(SensorMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    protected SensorMetadata nativeMetadata() {
        return metadata;
    }

    @Override
    public Multi<Void> doStart() {
        // nothing to do
        return null;
    }

    @Override
    protected void doUpdate(Void tick, Measures current, long lastUpdateEpoch, long newUpdateStartEpoch) {
        registeredPIDs().forEach(pid -> current.record(pid,
                new NoDurationSensorMeasure(new double[] { Math.random() }, lastUpdateEpoch, newUpdateStartEpoch)));
    }
}
