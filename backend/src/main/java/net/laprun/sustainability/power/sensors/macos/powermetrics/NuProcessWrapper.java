package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

public class NuProcessWrapper implements ProcessWrapper {
    private PowermetricsProcessHandler measureHandler;
    private String periodInMilliSecondsAsString;

    private NuProcess exec(PowermetricsProcessHandler handler) {
        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null");
        return new NuProcessBuilder(handler, handler.command()).start();
    }

    @Override
    public InputStream streamForMetadata() {
        final var metadataHandler = new PowermetricsProcessHandler(6500, "cpu_power", "-i", "10", "-n", "1");
        exec(metadataHandler);
        try {
            return metadataHandler.getInputStream().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(long periodInMilliSeconds) {
        this.periodInMilliSecondsAsString = Long.toString(periodInMilliSeconds);
    }

    @Override
    public void stop() {
        if (measureHandler != null) {
            measureHandler.stop();
            measureHandler = null;
        }
    }

    @Override
    public boolean isRunning() {
        return measureHandler != null && measureHandler.isRunning();
    }

    @Override
    public InputStream streamForMeasure() {
        if (!isRunning()) {
            measureHandler = new PowermetricsProcessHandler(27000, "cpu_power,tasks",
                    "--show-process-samp-norm", "--show-process-gpu", "-i",
                    periodInMilliSecondsAsString, "-n", "1");
            exec(measureHandler);
            try {
                return measureHandler.getInputStream().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Measure is still running");
    }
}
