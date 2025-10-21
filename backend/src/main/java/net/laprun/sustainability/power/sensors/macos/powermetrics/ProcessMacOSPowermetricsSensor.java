package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public class ProcessMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final ProcessWrapper processWrapper = new JavaProcessWrapper();

    public ProcessMacOSPowermetricsSensor() {
        // extract metadata
        try {
            initMetadata(processWrapper.streamForMetadata());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't extract sensor metadata", e);
        }
    }

    public void start(long frequency) throws Exception {
        processWrapper.start(frequency);
    }

    @Override
    public boolean isStarted() {
        return processWrapper.isRunning();
    }

    @Override
    protected InputStream getInputStream() {
        return processWrapper.streamForMeasure();
    }

    @Override
    public void stop() {
        processWrapper.stop();
    }
}
