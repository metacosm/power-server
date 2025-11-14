package net.laprun.sustainability.power.sensors;

import java.util.Map;
import java.util.Set;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

public abstract class AbstractPowerSensor<M extends Measures> implements PowerSensor {
    protected final M measures;
    private long lastUpdateEpoch;
    private boolean started;
    protected boolean cpuSharesEnabled = false;
    private SensorMetadata metadata;

    public AbstractPowerSensor(M measures) {
        this.measures = measures;
    }

    @Override
    public SensorMetadata metadata() {
        if (metadata == null) {
            metadata = nativeMetadata();
            if (cpuSharesEnabled) {
                metadata = SensorMetadata.from(metadata)
                        .withNewComponent(EXTERNAL_CPU_SHARE_COMPONENT_NAME,
                                "CPU share estimate based on currently configured strategy used in CPUShare", false,
                                SensorUnit.decimalPercentage)
                        .build();
            }
        }
        return metadata;
    }

    abstract protected SensorMetadata nativeMetadata();

    @Override
    public RegisteredPID register(long pid) {
        Log.debugf("Registered pid: %d", pid);
        return measures.register(pid);
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
        Log.debugf("Unregistered pid: %d", registeredPID.pid());
    }

    @Override
    public void start(long samplingFrequencyInMillis) throws Exception {
        if (!started) {
            lastUpdateEpoch = System.currentTimeMillis();
            started = true;
            doStart(samplingFrequencyInMillis);
        }
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void stop() {
        PowerSensor.super.stop();
        started = false;
    }

    @Override
    public Set<String> getRegisteredPIDs() {
        return measures.trackedPIDsAsString();
    }

    protected abstract void doStart(long samplingFrequencyInMillis);

    @Override
    public Measures update(Long tick, Map<String, Double> cpuShares) {
        final long newUpdateStartEpoch = System.currentTimeMillis();
        final var measures = doUpdate(lastUpdateEpoch, newUpdateStartEpoch, cpuShares);
        lastUpdateEpoch = measures.lastMeasuredUpdateEndEpoch() > 0 ? measures.lastMeasuredUpdateEndEpoch()
                : newUpdateStartEpoch;
        return measures;
    }

    protected long lastUpdateEpoch() {
        return lastUpdateEpoch;
    }

    abstract protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch, Map<String, Double> cpuShares);
}
