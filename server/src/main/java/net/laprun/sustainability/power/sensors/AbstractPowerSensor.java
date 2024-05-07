package net.laprun.sustainability.power.sensors;

public abstract class AbstractPowerSensor<M extends Measures> implements PowerSensor {
    protected final M measures;

    public AbstractPowerSensor(M measures) {
        this.measures = measures;
    }

    @Override
    public RegisteredPID register(long pid) {
        return measures.register(pid);
    }

    @Override
    public void unregister(RegisteredPID registeredPID) {
        measures.unregister(registeredPID);
    }
}
