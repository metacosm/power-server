package net.laprun.sustainability.power.sensors;

import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.SensorMetadata;

public abstract class AbstractPowerSensor implements PowerSensor {
    protected final Measures measures;
    private long lastUpdateEpoch;
    private boolean started;
    @ConfigProperty(name = "power-server.enable-cpu-share-sampling", defaultValue = "false")
    protected boolean cpuSharesEnabled;
    private SensorMetadata metadata;

    public AbstractPowerSensor(Measures measures) {
        this.measures = measures;
    }

    public AbstractPowerSensor() {
        this(new MapMeasures());
    }

    @Override
    public SensorMetadata metadata() {
        if (metadata == null) {
            metadata = nativeMetadata();
        }
        return metadata;
    }

    abstract protected SensorMetadata nativeMetadata();

    @Override
    public boolean wantsCPUShareSamplingEnabled() {
        return cpuSharesEnabled;
    }

    @Override
    public void enableCPUShareSampling(boolean enable) {
        cpuSharesEnabled = enable;
    }

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
    public void start() throws Exception {
        if (!started) {
            lastUpdateEpoch = System.currentTimeMillis();
            started = true;
            doStart();
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

    protected abstract void doStart();

    @Override
    public Measures update(long tick) {
        final long newUpdateStartEpoch = System.currentTimeMillis();
        Log.debugf("Sensor update last called: %dms ago", newUpdateStartEpoch - lastUpdateEpoch);
        final var measures = doUpdate(lastUpdateEpoch, newUpdateStartEpoch);
        lastUpdateEpoch = newUpdateStartEpoch;
        return measures;
    }

    protected long lastUpdateEpoch() {
        return lastUpdateEpoch;
    }

    abstract protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch);
}
