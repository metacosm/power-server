package net.laprun.sustainability.power.sensors.test;

import static net.laprun.sustainability.power.SensorUnit.mW;

import java.util.List;

import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.AbstractPowerSensor;
import net.laprun.sustainability.power.sensors.MapMeasures;
import net.laprun.sustainability.power.sensors.Measures;

@SuppressWarnings("unused")
public class TestPowerSensor extends AbstractPowerSensor<MapMeasures> {
    public static final String CPU = "cpu";
    public static final SensorMetadata DEFAULT = new SensorMetadata(
            List.of(new SensorMetadata.ComponentMetadata(CPU, 0, "CPU", true, mW)),
            "Test PowerSensor returning random values for a single 'cpu' component");
    private final SensorMetadata metadata;

    public TestPowerSensor() {
        this(DEFAULT);
    }

    public TestPowerSensor(SensorMetadata metadata) {
        super(new MapMeasures());
        this.metadata = metadata;
    }

    @Override
    public SensorMetadata metadata() {
        return metadata;
    }

    @Override
    public void doStart(long samplingFrequencyInMillis) {
        // nothing to do
    }

    @Override
    protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch) {
        measures.trackedPIDs().forEach(pid -> measures.record(pid,
                new SensorMeasure(new double[] { Math.random() }, lastUpdateEpoch, newUpdateStartEpoch)));
        return measures;
    }
}
