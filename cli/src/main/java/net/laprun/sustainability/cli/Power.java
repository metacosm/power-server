package net.laprun.sustainability.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import net.laprun.sustainability.power.Measure;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.total.TotalSyntheticComponent;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.macos.powermetrics.FileMacOSPowermetricsSensor;
import picocli.CommandLine;

@CommandLine.Command
public class Power implements Runnable {

    @CommandLine.Parameters(index = "0..*", hidden = true)
    List<String> cmd;

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Optional name for the application, defaults to passed command line")
    String name;

    private final File output;
    private final PowerSensor sensor;
    private Process powermetrics;

    public Power() throws IOException {
        /*
         * this.output = File.createTempFile("power-", ".tmp");
         * output.deleteOnExit();
         */
        this.output = new File("output.txt");
        output.createNewFile();
        sensor = new FileMacOSPowermetricsSensor(output);
        Log.info(output.getAbsolutePath());
    }

    @Override
    public void run() {
        if (cmd == null || cmd.isEmpty()) {
            Log.info("No command specified, exiting.");
            return;
        }

        final var cmdPid = Uni.createFrom()
                .item(this::runPowermetrics)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .chain(powermetrics -> Uni.createFrom()
                        .item(this::runCommandToMeasure)
                        .onTermination()
                        .invoke(powermetrics::destroy))
                .await()
                .indefinitely();

        final int exit;
        try {
            Log.infof("powermetrics exit code: %d", powermetrics.waitFor());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        extractPowerConsumption(cmdPid);

        Quarkus.waitForExit();
    }

    private Measure extractPowerConsumption(long pid) {
        sensor.stop();
        // first read metadata
        final var metadata = sensor.metadata();
        Log.infof("Metadata:\n%s", metadata);
        // register pid
        final var registeredPID = sensor.register(pid);
        // re-open output file to read process info
        final var measure = sensor.update(0L);
        final var power = new TotalSyntheticComponent(metadata, SensorUnit.W, 0, 1, 2)
                .asMeasure(measure.getOrDefault(registeredPID).components());

        Log.info("Measured power: " + power);
        Quarkus.asyncExit();

        return power;
    }

    private Process runPowermetrics() {
        Log.info("Starting powermetrics");
        try {
            powermetrics = new ProcessBuilder()
                    .command("powermetrics", "--samplers",
                            "cpu_power,tasks",
                            "--show-process-samp-norm",
                            "--show-process-gpu",
                            "--show-usage-summary",
                            "-i", "0") // no sampling frequency, just record aggregate
                    .redirectOutput(output)
                    .start();
            return powermetrics;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long runCommandToMeasure() {
        Log.infof("Recording energy consumption for: %s", cmd);
        final var command = new ProcessBuilder().command(cmd);
        try {
            final var process = command.start();
            process.waitFor();
            return process.pid();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
