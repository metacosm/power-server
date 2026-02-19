package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import io.smallrye.mutiny.Multi;

/**
 * The aim of this sensor is to only perform one long measure and then read the power information from it once done,
 */
public class FileMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private final File file;

    public FileMacOSPowermetricsSensor(File file) {
        this.file = file;
    }

    @Override
    protected Multi<InputStream> getInputStream() {
        return Multi.createFrom().item(fromFile());
    }

    private FileInputStream fromFile() {
        try {
            return new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        // need to defer reading metadata until we know the file has been populated
        initMetadata(fromFile());
        super.stop();
    }
}
