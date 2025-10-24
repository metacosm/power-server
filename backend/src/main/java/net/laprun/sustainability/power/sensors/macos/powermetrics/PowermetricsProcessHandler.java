package net.laprun.sustainability.power.sensors.macos.powermetrics;

import net.laprun.sustainability.power.nuprocess.OutputRecordingProcessHandler;

public class PowermetricsProcessHandler extends OutputRecordingProcessHandler {
    public PowermetricsProcessHandler(String... command) {
        super(command);
    }

    @Override
    protected String[] initCommand(String... command) {
        return ProcessWrapper.preparePowermetricsCommand(command);
    }
}
