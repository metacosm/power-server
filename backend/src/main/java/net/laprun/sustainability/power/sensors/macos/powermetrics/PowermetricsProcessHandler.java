package net.laprun.sustainability.power.sensors.macos.powermetrics;

import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;

public class PowermetricsProcessHandler extends BaseProcessHandler {
    public PowermetricsProcessHandler(String... command) {
        super(command);
    }

    @Override
    protected String[] initCommand(String... command) {
        final var additionalArgsCardinality = 3;
        final var args = new String[command.length + additionalArgsCardinality];
        args[0] = "sudo";
        args[1] = "powermetrics";
        args[2] = "--samplers";
        System.arraycopy(command, 0, args, additionalArgsCardinality, command.length);
        return args;
    }
}
