package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;

public class NuProcessWrapper {
    @SuppressWarnings("UnusedReturnValue")
    public static NuProcess exec(BaseProcessHandler handler) {
        if (handler == null)
            throw new IllegalArgumentException("Handler cannot be null");
        return new NuProcessBuilder(handler, handler.command()).start();
    }

    public static InputStream metadataInputStream() {
        return waitForOutput(new PowermetricsProcessHandler(6500, "cpu_power", "-i", "10", "-n", "1"));
    }

    public static CompletableFuture<InputStream> asyncGetOutput(PowermetricsProcessHandler handler) {
        exec(handler);
        return handler.getInputStream();
    }

    public static InputStream waitForOutput(PowermetricsProcessHandler handler) {
        try {
            final var start = System.currentTimeMillis();
            final var inputStream = asyncGetOutput(handler).get();
            Log.infof("Waited for output: %dms", System.currentTimeMillis() - start);
            return inputStream;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream measureInputStream(String periodInMilliSecondsAsString) {
        final var inputStream = waitForOutput(new PowermetricsProcessHandler(27000, "cpu_power,tasks",
                "--show-process-samp-norm", "--show-process-gpu", "-i",
                periodInMilliSecondsAsString, "-n", "1"));
        Log.infof("powermetrics output: %s", inputStream);
        return inputStream;
    }
}
