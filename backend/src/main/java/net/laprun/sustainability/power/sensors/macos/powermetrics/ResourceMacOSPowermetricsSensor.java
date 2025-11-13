package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;
import java.util.Map;

import net.laprun.sustainability.power.sensors.Measures;

public class ResourceMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final String resourceName;
    private final long start;

    public ResourceMacOSPowermetricsSensor(String resourceName) {
        this(resourceName, -1);
    }

    ResourceMacOSPowermetricsSensor(String resourceName, long expectedStartUpdateEpoch) {
        cpuSharesEnabled = false;
        this.resourceName = resourceName;
        this.start = expectedStartUpdateEpoch;
        initMetadata(getInputStream());
    }

    @Override
    protected Measures doUpdate(long lastUpdateEpoch, long newUpdateStartEpoch, Map<String, Double> cpuShares) {
        // use the expected start measured time (if provided) for the measure instead of using the provided current epoch
        return super.doUpdate(start != -1 ? start : lastUpdateEpoch, newUpdateStartEpoch, cpuShares);
    }

    @Override
    protected InputStream getInputStream() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }
}
