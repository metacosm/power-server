package net.laprun.sustainability.cli;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.quarkus.logging.Log;
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

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Path outputFile;
    private final PowerSensor sensor;
    private Process powermetrics;

    public Power() throws IOException {
        this.outputFile = Path.of("output.txt").toAbsolutePath();
        Files.deleteIfExists(this.outputFile);
        //        Files.createFile(this.outputFile);

        Log.infof("Output file: %s", this.outputFile);
        sensor = new FileMacOSPowermetricsSensor(this.outputFile.toFile());
    }

    @Override
    public void run() {
        if (cmd == null || cmd.isEmpty()) {
            Log.info("No command specified, exiting.");
            return;
        }

        var powermetricsProcess = runPowermetrics();
        var powermetricsPid = powermetricsProcess.pid();
        Log.infof("Powermetrics pid: %d", powermetricsPid);

        var commandProcess = runCommandToMeasure();
        var commandPid = commandProcess.pid();

        try {
            var commandExitVal = commandProcess.waitFor();
            Log.infof("Process [%s] with pid %d exited with status code %d", cmd, commandPid, commandExitVal);

            stopPowermetrics(commandPid);
            //            var latch = new CountDownLatch(1);
            //            extractPowerConsumption(commandPid, latch);
            //            latch.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Measure extractPowerConsumption(long pid, CountDownLatch latch) {
        Log.infof("Extracting power consumption for process %d", pid);
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

        latch.countDown();

        return power;
    }

    private void stopPowermetrics(long commandPid) {
        var powermetricsPid = powermetrics.pid();
        var powermetricsExit = powermetrics.onExit().thenApply(p -> {
            try {
                Log.info("powermetrics process complete");

                await()
                        .atMost(Duration.ofMinutes(1))
                        .until(() -> Files.exists(this.outputFile)
                                && Files.readString(this.outputFile).contains("Combined Power (CPU + GPU + ANE):"));

                var latch = new CountDownLatch(1);
                extractPowerConsumption(commandPid, latch);

                latch.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return p;
        });

        try {
            var exitVal = invokeOnAnotherThread(() -> {
                Log.infof("Sending SIGINT to powermetrics process %d", powermetricsPid);
                return new ProcessBuilder("kill", "-SIGINT", String.valueOf(powermetricsPid));
            }).waitFor();
            Log.infof("Sent SIGINT to powermetrics. Received exit status %d", exitVal);

            var powermetricsExitStatus = powermetricsExit.get().waitFor();
            Log.infof("powermetrics process %d exited with status %d", powermetricsPid, powermetricsExitStatus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Process runPowermetrics() {
        return powermetrics = invokeOnAnotherThread(() -> {
            Log.infof("Starting powermetrics - outputting to %s", this.outputFile);

            return new ProcessBuilder()
                    .command("powermetrics", "--samplers",
                            "cpu_power,tasks",
                            "--show-process-samp-norm",
                            "--show-process-gpu",
                            "--show-usage-summary",
                            "-i", "0") // no sampling frequency, just record aggregate
                    .redirectOutput(this.outputFile.toFile());
            //                    .inheritIO();
            //                            "-o", this.outputFile.toString());
        });
    }

    private Process runCommandToMeasure() {
        return invokeOnAnotherThread(() -> {
            Log.infof("Recording energy consumption for: %s", cmd);
            return new ProcessBuilder().command(cmd);
        });
    }

    private Process invokeOnAnotherThread(Callable<ProcessBuilder> processBuilder) {
        try {
            return this.executor.submit(() -> processBuilder.call().start()).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
