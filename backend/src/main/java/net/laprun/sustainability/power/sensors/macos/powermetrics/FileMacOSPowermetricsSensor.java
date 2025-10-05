package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * The aim of this sensor is to only perform one long measure and then read the power information from it once done,
 */
public class FileMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final File file;
    private boolean started;

    public FileMacOSPowermetricsSensor(File file) {
        this.file = file;
    }

    @Override
    protected InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public void stop() {
        started = false;
        initMetadata(getInputStream());
    }
}
