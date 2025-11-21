package net.laprun.sustainability.power.measures;

import net.laprun.sustainability.power.SensorMeasure;

public record ExternalCPUShareSensorMeasure(SensorMeasure delegate, double cpuShare) implements SensorMeasure {
    @Override
    public double[] components() {
        return delegate.components();
    }

    @Override
    public long startMs() {
        return delegate.startMs();
    }

    @Override
    public long endMs() {
        return delegate.endMs();
    }

    @Override
    public long durationMs() {
        return delegate.durationMs();
    }

    @Override
    public boolean isPartial() {
        return delegate.isPartial();
    }

    @Override
    public double externalCPUShare() {
        return cpuShare;
    }
}
