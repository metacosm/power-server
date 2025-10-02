package net.laprun.sustainability.cli;

import java.util.List;

import net.laprun.sustainability.power.PowerMeasurer;
import picocli.CommandLine;

@CommandLine.Command
public class Power implements Runnable {

    @CommandLine.Parameters(hidden = true)
    List<String> cmd;

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Optional name for the application, defaults to passed command line")
    String name;

    private final PowerMeasurer measurer;

    public Power(PowerMeasurer measurer) {
        this.measurer = measurer;
    }

    @Override
    public void run() {
        final var command = new ProcessBuilder().command(cmd);
        final var start = System.nanoTime();
        try {
            final var process = command.start();
            var duration = System.nanoTime() - start;
            System.out.println("start duration: " + duration);
            final var processTracker = measurer.startTrackingProcess(process);
            process.onExit().thenAccept(unused -> processTracker.cancel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
