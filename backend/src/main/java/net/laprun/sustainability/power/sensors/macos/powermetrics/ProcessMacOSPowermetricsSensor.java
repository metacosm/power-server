package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public class ProcessMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final ProcessWrapper processWrapper = new NuProcessWrapper();

    public ProcessMacOSPowermetricsSensor() {
        // extract metadata
        try {
            initMetadata(processWrapper.streamForMetadata());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't extract sensor metadata", e);
        }
    }

    @Override
    public void doStart(long frequency) {
        processWrapper.start(frequency);
    }

    @Override
    protected InputStream getInputStream() {
        return processWrapper.streamForMeasure();
    }

    @Override
    public void stop() {
        processWrapper.stop();
        super.stop();
    }
}
