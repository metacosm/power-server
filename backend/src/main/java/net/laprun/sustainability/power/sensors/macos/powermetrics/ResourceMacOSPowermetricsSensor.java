package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

import io.smallrye.mutiny.Multi;
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
        initMetadata(fromResource());
    }

    @Override
    protected void doUpdate(InputStream inputStream, Measures current, long lastUpdateEpoch, long newUpdateStartEpoch) {
        // use the expected start measured time (if provided) for the measure instead of using the provided current epoch
        super.doUpdate(inputStream, current, start != -1 ? start : lastUpdateEpoch, newUpdateStartEpoch);
    }

    @Override
    protected Multi<InputStream> getInputStream() {
        return Multi.createFrom().item(fromResource());
    }

    InputStream fromResource() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
    }
}
