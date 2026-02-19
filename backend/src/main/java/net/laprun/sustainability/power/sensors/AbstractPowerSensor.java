package net.laprun.sustainability.power.sensors;

import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import net.laprun.sustainability.power.SensorMetadata;

public abstract class AbstractPowerSensor<T> implements PowerSensor {
    private final PIDRegistry registry = new PIDRegistry();
    private final Measures current = new MapMeasures();
    private long lastUpdateEpoch;
    private boolean started;
    private Multi<Measures> measures;
    @ConfigProperty(name = "power-server.enable-cpu-share-sampling", defaultValue = "false")
    protected boolean cpuSharesEnabled;
    private SensorMetadata metadata;

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
        final var key = RegisteredPID.create(pid);
        registry.register(key);
        return key;
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        registry.unregister(registeredPID);
        Log.debugf("Unregistered pid: %d", registeredPID.pid());
    }

    protected int numberOfRegisteredPIDs() {
        return registry.size();
    }

    @Override
    public Set<RegisteredPID> registeredPIDs() {
        return registry.pids();
    }

    @Override
    public Set<String> registeredPIDsAsStrings() {
        return registry.pidsAsStrings();
    }

    @Override
    public Multi<Measures> start() throws Exception {
        if (!started) {
            lastUpdateEpoch = System.currentTimeMillis();
            started = true;
            measures = doStart()
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .onItem()
                    .transform(this::update);
        }
        return measures;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void stop() {
        if (started) {
            PowerSensor.super.stop();
            started = false;
        }
    }

    public Measures update(T tick) {
        // reset revolving measure so that we don't get values for pids that are not tracked anymore
        current.clear();
        final long newUpdateStartEpoch = System.currentTimeMillis();
        Log.infof("Sensor update last called: %dms ago", newUpdateStartEpoch - lastUpdateEpoch);
        Log.infof("input %s", tick);
        // extract current values into revolving measure
        doUpdate(tick, current, lastUpdateEpoch, newUpdateStartEpoch);
        Log.infof("Last recorded measure: %s", current);
        lastUpdateEpoch = newUpdateStartEpoch;
        return current;
    }

    protected abstract Multi<T> doStart();

    protected long lastUpdateEpoch() {
        return lastUpdateEpoch;
    }

    abstract protected void doUpdate(T tick, Measures current, long lastUpdateEpoch, long newUpdateStartEpoch);
}
