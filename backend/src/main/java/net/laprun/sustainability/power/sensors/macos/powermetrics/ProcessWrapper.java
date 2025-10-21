package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public interface ProcessWrapper {
    InputStream streamForMetadata();

    void start(long periodInMilliSeconds);

    void stop();

    boolean isRunning();

    InputStream streamForMeasure();
}
