package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public class ProcessMacOSPowermetricsSensor extends MacOSPowermetricsSensor {
    private Process powermetrics;

    public ProcessMacOSPowermetricsSensor() {
        // extract metadata
        try {
            final var exec = new ProcessBuilder()
                    .command("powermetrics", "--samplers", "cpu_power", "-i", "10", "-n", "1")
                    .start();

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

            initMetadata(exec.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't extract sensor metadata", e);
        }
    }

    public void start(long frequency) throws Exception {
        if (!isStarted()) {
            // it takes some time for the external process in addition to the sampling time so adjust the sampling frequency to account for this so that at most one measure occurs during the sampling time window
            final var freq = Long.toString(frequency - 50);
            powermetrics = new ProcessBuilder().command("powermetrics", "--samplers", "cpu_power,tasks",
                    "--show-process-samp-norm", "--show-process-gpu", "-i", freq).start();
        }
    }

    @Override
    public boolean isStarted() {
        return powermetrics != null && powermetrics.isAlive();
    }

    @Override
    protected InputStream getInputStream() {
        return powermetrics.getInputStream();
    }

    @Override
    public void stop() {
        powermetrics.destroy();
    }
}
