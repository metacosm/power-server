package net.laprun.sustainability.power.sensors;

import io.quarkus.logging.Log;

public abstract class AbstractPowerSensor<M extends Measures> implements PowerSensor {
    protected final M measures;
    private long lastUpdateEpoch;
    private boolean started;

    public AbstractPowerSensor(M measures) {
        this.measures = measures;
    }

    @Override
    public RegisteredPID register(long pid) {
        if (Measures.SYSTEM_TOTAL_PID == pid) {
            return Measures.SYSTEM_TOTAL_REGISTERED_PID;
        }
        Log.info("Registered pid: " + pid);
        return measures.register(pid);
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        if (Measures.SYSTEM_TOTAL_REGISTERED_PID != registeredPID) {
            measures.unregister(registeredPID);
            Log.info("Unregistered pid: " + registeredPID.pid());
        }
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

    protected abstract void doStart(long samplingFrequencyInMillis);

    @Override
    public Measures update(Long tick) {
        final long newUpdateStartEpoch = System.currentTimeMillis();
        final var measures = doUpdate(lastUpdateEpoch, newUpdateStartEpoch);
        lastUpdateEpoch = measures.lastMeasuredUpdateEndEpoch() > 0 ? measures.lastMeasuredUpdateEndEpoch()
                : newUpdateStartEpoch;
        return measures;
    }

    protected long lastUpdateEpoch() {
        return lastUpdateEpoch;
    }

    abstract protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch);
}
