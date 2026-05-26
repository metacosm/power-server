package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

import io.smallrye.mutiny.Multi;

public class ProcessMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private static final int MINIMAL_PERIOD_MS = 700;
    private long samplingPeriodInMillis;
    private String periodInMilliSecondsAsString;

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
        this.periodInMilliSecondsAsString = Long.toString(samplingPeriodInMillis);
        return samplingPeriodInMillis;
    }

    @Override
    protected Multi<InputStream> getInputStream() {
        /*
         * return Multi.createBy()
         * .repeating()
         * .completionStage(() -> {
         * Log.info("getInputStream() called");
         * final var measureHandler = new PowermetricsProcessHandler(27000, "cpu_power,tasks",
         * "--show-process-samp-norm", "--show-process-gpu", "-i",
         * periodInMilliSecondsAsString, "-n", "1");
         * NuProcessWrapper.exec(measureHandler);
         * return measureHandler.getInputStream();
         * })
         * .indefinitely();
         */
        return Multi.createBy()
                .repeating()
                .supplier(() -> NuProcessWrapper.measureInputStream(periodInMilliSecondsAsString))
                .indefinitely();
    }

    @Override
    public void stop() {
        if (isStarted()) {
            super.stop();
        }
    }
}
