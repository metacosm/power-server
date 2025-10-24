package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.io.InputStream;

public interface ProcessWrapper {
    InputStream streamForMetadata();

    void start(long periodInMilliSeconds);

    void stop();

    boolean isRunning();

    InputStream streamForMeasure();

    static String[] preparePowermetricsCommand(String[] command) {
        final var additionalArgsCardinality = 5;
        final var args = new String[command.length + additionalArgsCardinality];
        args[0] = "sudo";
        args[1] = "powermetrics";
        args[2] = "--order";
        args[3] = "cputime";
        args[4] = "--samplers";
        System.arraycopy(command, 0, args, additionalArgsCardinality, command.length);
        return args;
    }
}
