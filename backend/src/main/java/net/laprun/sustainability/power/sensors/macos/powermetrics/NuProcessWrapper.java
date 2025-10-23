package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

public class NuProcessWrapper implements ProcessWrapper {
    private final PowermetricsProcessHandler metadataHandler;
    private PowermetricsProcessHandler measureHandler;
    private String periodInMilliSecondsAsString;

    public NuProcessWrapper() {
        metadataHandler = new PowermetricsProcessHandler("cpu_power", "-i", "10", "-n", "1");
    }

    private NuProcess exec(PowermetricsProcessHandler handler) {
        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null");
        return new NuProcessBuilder(handler, handler.comand()).start();
    }

    @Override
    public InputStream streamForMetadata() {
        exec(metadataHandler);
        try {
            return metadataHandler.getInputStream().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(long periodInMilliSeconds) {
        // todo? check if asked period is the same as the current used one
        periodInMilliSeconds = periodInMilliSeconds > 100 ? periodInMilliSeconds - 50 : periodInMilliSeconds;
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
            measureHandler = new PowermetricsProcessHandler("cpu_power,tasks",
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
