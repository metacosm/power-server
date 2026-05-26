package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.AbstractPowerSensor;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.RegisteredPID;

/**
 * A macOS powermetrics based {@link PowerSensor} implementation.
 */
public abstract class MacOSPowermetricsSensor extends AbstractPowerSensor<InputStream> {
    /**
     * The Central Processing Unit component name
     */
    public static final String CPU = "CPU";
    /**
     * The Graphics Procssing Unit component name
     */
    public static final String GPU = "GPU";
    /**
     * The Apple Neural Engine component name
     */
    public static final String ANE = "ANE";
    /**
     * The Dynamic Random Access Memory component name
     */
    @SuppressWarnings("unused")
    public static final String DRAM = "DRAM";
    @SuppressWarnings("unused")
    public static final String DCS = "DCS";
    /**
     * The package component name
     */
    public static final String PACKAGE = "Package";
    /**
     * The extracted CPU share component name, this represents the process' share of the measured power consumption
     */
    public static final String CPU_SHARE = "cpuShare";

    private CPU cpu;
    private long lastCalled = System.currentTimeMillis();

    @Override
    public boolean supportsProcessAttribution() {
        return true;
    }

    void initMetadata(InputStream inputStream) {
        cpu = PowerMetricsParser.initCPU(inputStream);
    }

    @Override
    protected SensorMetadata nativeMetadata() {
        return cpu.metadata();
    }

    @Override
    protected Multi<InputStream> doStart() {
        return getInputStream();
    }

    void extractPowerMeasure(InputStream powerMeasureInput, Measures current, long lastUpdateEpoch, long newUpdateEpoch) {
        if (Log.isDebugEnabled()) {
            final var start = System.currentTimeMillis();
            Log.debugf("powermetrics measure extraction last called %dms ago", (start - lastCalled));
            lastCalled = start;
        }
        PowerMetricsParser.extractPowerMeasure(powerMeasureInput, current, lastUpdateEpoch, newUpdateEpoch, registeredPIDs(),
                metadata(), cpu);
    }

    @Override
    protected void doUpdate(InputStream inputStream, Measures current, long lastUpdateEpoch, long newUpdateStartEpoch) {
        extractPowerMeasure(inputStream, current, lastUpdateEpoch, newUpdateStartEpoch);
    }

    protected abstract Multi<InputStream> getInputStream();

    @Override
    public void unregister(RegisteredPID registeredPID) {
        super.unregister(registeredPID);
        // if we're not tracking any processes anymore, stop powermetrics as well
        if (numberOfRegisteredPIDs() == 0) {
            stop();
        }
    }
}
