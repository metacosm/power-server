package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;

public class NuProcessWrapper implements ProcessWrapper {
    private PowermetricsProcessHandler measureHandler;
    private String periodInMilliSecondsAsString;

    @SuppressWarnings("UnusedReturnValue")
    public static NuProcess exec(BaseProcessHandler handler) {
        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null");
        return new NuProcessBuilder(handler, handler.command()).start();
    }

    public static InputStream metadataInputStream() {
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
