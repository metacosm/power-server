package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.IOException;
import java.io.InputStream;

import io.quarkus.logging.Log;

public class JavaProcessWrapper implements ProcessWrapper {
    private Process powermetrics;

    @Override
    public InputStream streamForMetadata() {
        final Process exec;
        try {
            exec = execPowermetrics("cpu_power", "-i", "10", "-n", "1");
            exec.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return exec.getInputStream();
    }

    @Override
    public void start(long periodInMilliSeconds) {
        if (!isRunning()) {
            // it takes some time for the external process in addition to the sampling time so adjust the sampling frequency to account for this so that at most one measure occurs during the sampling time window
            periodInMilliSeconds = periodInMilliSeconds > 100 ? periodInMilliSeconds - 50 : periodInMilliSeconds;
            final var freq = Long.toString(periodInMilliSeconds);
            try {
                powermetrics = execPowermetrics("cpu_power,tasks", "--show-process-samp-norm", "--show-process-gpu", "-i",
                        freq);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void stop() {
        powermetrics.destroy();
    }

    @Override
    public boolean isRunning() {
        return powermetrics != null && powermetrics.isAlive();
    }

    @Override
    public InputStream streamForMeasure() {
        return powermetrics.getInputStream();
    }

    public Process execPowermetrics(String... options) throws IOException {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("No powermetrics options specified");
        }
        final var additionalArgsCardinality = 3;
        final var args = new String[options.length + additionalArgsCardinality];
        args[0] = "sudo";
        args[1] = "powermetrics";
        args[2] = "--samplers";
        System.arraycopy(options, 0, args, additionalArgsCardinality, options.length);
        final var start = System.currentTimeMillis();
        final Process exec = new ProcessBuilder().command(args).start();
        Log.info("Starting process took " + (System.currentTimeMillis() - start) + "ms");
        // if the process is already dead, get the error
        if (!exec.isAlive()) {
            final var exitValue = exec.exitValue();
            if (exitValue != 0) {
                final String errorMsg;
                try (final var error = exec.errorReader()) {
                    errorMsg = error.readLine();
                }
                throw new RuntimeException(
                        "Couldn't execute powermetrics. Error code: " + exitValue + ", message: " + errorMsg);
            }
        }
        return exec;
    }

}
