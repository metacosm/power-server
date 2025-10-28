package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

import net.laprun.sustainability.power.sensors.Measures;

public class ResourceMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final String resourceName;
    private boolean started;
    private final long start;

    public ResourceMacOSPowermetricsSensor(String resourceName) {
        this(resourceName, -1);
    }

    ResourceMacOSPowermetricsSensor(String resourceName, long expectedStartUpdateEpoch) {
        this.resourceName = resourceName;
        this.start = expectedStartUpdateEpoch;
        initMetadata(getInputStream());
    }

    @Override
    protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch) {
        // use the expected start measured time (if provided) for the measure instead of using the provided current epoch
        return super.doUpdate(start != -1 ? start : lastUpdateEpoch, newUpdateStartEpoch);
    }

    @Override
    protected InputStream getInputStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start(long samplingFrequencyInMillis) {
        if (!started) {
            started = true;
        }
    }
}
