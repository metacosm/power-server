package net.laprun.sustainability.power.sensors;

import io.quarkus.logging.Log;

public abstract class AbstractPowerSensor<M extends Measures> implements PowerSensor {
    //    private static final Logger log = Logger.getLogger(AbstractPowerSensor.class);
    protected final M measures;

    public AbstractPowerSensor(M measures) {
        this.measures = measures;
    }

    @Override
    public RegisteredPID register(long pid) {
        Log.info("Registered pid: " + pid);
        return measures.register(pid);
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
        Log.info("Unregistered pid: " + registeredPID.pid());
    }
}
