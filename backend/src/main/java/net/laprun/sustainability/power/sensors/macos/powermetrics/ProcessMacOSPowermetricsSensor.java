package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public class ProcessMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private static final int MINIMAL_PERIOD_MS = 700;
    private final ProcessWrapper processWrapper = new NuProcessWrapper();
    private long samplingPeriodInMillis;

    public ProcessMacOSPowermetricsSensor() {
        // extract metadata
        try {
            initMetadata(NuProcessWrapper.metadataInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't extract sensor metadata", e);
        }
    }

    @Override
    public long adjustSamplingPeriodIfNeeded(long requestedSamplingPeriodInMillis) {
        if (requestedSamplingPeriodInMillis < MINIMAL_PERIOD_MS) {
            throw new IllegalArgumentException(
                    "powermetrics takes around 500ms of incompressible time for each sample, set the sampling period to at least "
                            +
                            MINIMAL_PERIOD_MS + " milliseconds to get useful results to also account for processing time.");
        }
        samplingPeriodInMillis = requestedSamplingPeriodInMillis - 600;
        return samplingPeriodInMillis;
    }

    @Override
    public void doStart() {
        super.doStart();
        processWrapper.start(samplingPeriodInMillis);
    }

    @Override
    protected InputStream getInputStream() {
        return processWrapper.streamForMeasure();
    }

    @Override
    public void stop() {
        if (isStarted()) {
            processWrapper.stop();
            super.stop();
        }
    }
}
