package net.laprun.sustainability.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.quarkus.logging.Log;
import net.laprun.sustainability.power.Measure;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.total.TotalSyntheticComponent;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.macos.powermetrics.FileMacOSPowermetricsSensor;
import picocli.CommandLine;

@CommandLine.Command
public class Power implements Runnable {

    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Optional name for the application, defaults to passed command line")
    String name;

    @CommandLine.Option(names = "-p", description = "Optional process id of process to measure consumption for")
    String pid;

    @CommandLine.Option(names = "-c", description = "Command to measure energy consumption for")
    String command;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private final Path outputFile;
    private final PowerSensor sensor;

    private record ProcessId(long pid) {
    }

    public Power() throws IOException {
        this.outputFile = Path.of("output.txt").toAbsolutePath();
        Files.deleteIfExists(this.outputFile);
        Log.infof("Output file: %s", this.outputFile);
        sensor = new FileMacOSPowermetricsSensor(this.outputFile.toFile());
    }

    private void checkInputs() {
        if ((pid == null) && (command == null)) {
            throw new IllegalArgumentException("Must provide either -p or -c");
        }
    }

    @Override
    public void run() {
        checkInputs();

        var powermetricsProcess = runPowermetrics();
        var powermetricsPid = powermetricsProcess.pid();
        Log.infof("Powermetrics pid: %d", powermetricsPid);

        var commandProcessId = runCommandToMeasure();
        Log.infof("Process pid: %d", commandProcessId.pid());

        if (configWithExistingProcess()) {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            var commandProcessHandle = ProcessHandle.of(commandProcessId.pid())
                    .filter(ProcessHandle::isAlive)
                    .map(p -> p.onExit().join())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Process %s is not alive".formatted(commandProcessId.pid())));

            // By now the underlying process should be done
            Log.infof("Process [%s] with pid %d exited", commandProcessHandle.info().commandLine().orElse(""),
                    commandProcessId.pid());
        }

        stopPowermetrics(powermetricsProcess, commandProcessId);
    }

    private Measure extractPowerConsumption(ProcessId processId, CountDownLatch latch) {
        var pid = processId.pid();
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

        //        latch.countDown();

        return power;
    }

    private void stopPowermetrics(Process powermetricsProcess, Power.ProcessId commandPid) {
        var powermetricsPid = powermetricsProcess.pid();
        //    var powermetricsExit = powermetricsProcess.onExit().join();
        try {
            Log.info("powermetrics process complete");

            //            await()
            //                    .atMost(Duration.ofMinutes(1))
            //                    .until(() -> Files.exists(this.outputFile)
            //                            && Files.readString(this.outputFile).contains("Combined Power (CPU + GPU + ANE):"));

            //            var latch = new CountDownLatch(1);
            //            extractPowerConsumption(commandPid, latch);

            //            latch.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Log.infof("Sending SIGIO to powermetrics process %d", powermetricsPid);
            new ProcessBuilder("bash", "-c", "kill -SIGIO %s".formatted(String.valueOf(powermetricsPid))).start().waitFor();
            var exitVal = invokeOnAnotherThread(() -> {
                Log.infof("Sending SIGTERM to powermetrics process %d", powermetricsPid);
                return new ProcessBuilder("bash", "-c", "kill %s".formatted(String.valueOf(powermetricsPid)));
            }).waitFor();
            Log.infof("Sent SIGTERM to powermetrics. Received exit status %d", exitVal);

            var powermetricsExitStatus = powermetricsProcess.onExit().get().waitFor();
            //      var powermetricsExitStatus = powermetricsExit.get().waitFor();
            Log.infof("powermetrics process %d exited with status %d", powermetricsPid, powermetricsProcess.exitValue());

            Log.infof("Output file: %s", Files.readString(this.outputFile));
            extractPowerConsumption(commandPid, new CountDownLatch(0));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Process runPowermetrics() {
        return invokeOnAnotherThread(() -> {
            Log.infof("Starting powermetrics - outputting to %s", this.outputFile);

            return new ProcessBuilder()
                    .command(
                            List.of("sudo", "bash", "-c",
                                    "powermetrics --samplers cpu_power,tasks --show-process-samp-norm --show-process-gpu --show-usage-summary -i 0"))
                    //                    .command("sudo", "powermetrics", "--samplers",
                    //                            "cpu_power,tasks",
                    //                            "--show-process-samp-norm",
                    //                            "--show-process-gpu",
                    //                            "--show-usage-summary",
                    //                            "-i", "0") // no sampling frequency, just record aggregate
                    .redirectOutput(this.outputFile.toFile());
        });
    }

    private static Optional<String> stripped(String s) {
        return Optional.ofNullable(s)
                .map(String::strip)
                .filter(s2 -> !s2.isBlank());
    }

    private Optional<Power.ProcessId> pidProcessId() {
        return stripped(this.pid)
                .map(Long::parseLong)
                .map(ProcessId::new);
    }

    private Optional<ProcessId> commandProcessId() {
        return stripped(this.command)
                //                .map(s -> s.split("\\s+"))
                .map(cmd -> invokeOnAnotherThread(() -> {
                    Log.infof("Invoking command: %s", cmd);
                    return new ProcessBuilder().command("bash", "-c", cmd);
                }))
                .map(p -> new ProcessId(p.pid()));
    }

    private ProcessId runCommandToMeasure() {
        return pidProcessId()
                .or(this::commandProcessId)
                .orElseThrow(() -> new IllegalArgumentException("Must provide either -p or -c"));
    }

    private boolean configWithExistingProcess() {
        return pidProcessId().isPresent();
    }

    private static Process invokeOnAnotherThread(Callable<ProcessBuilder> processBuilder) {
        try {
            // Trying with everything on the same thread
            return processBuilder.call().start();
            //            return EXECUTOR.submit(() -> processBuilder.call().start()).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
