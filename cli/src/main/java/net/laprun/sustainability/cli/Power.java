package net.laprun.sustainability.cli;

import java.io.IOException;
import java.util.Optional;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import net.laprun.sustainability.power.Measure;
import net.laprun.sustainability.power.Security;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.total.TotalSyntheticComponent;
import picocli.CommandLine;

@CommandLine.Command
public class Power implements Runnable {
    @CommandLine.Option(names = { "-n",
            "--name" }, description = "Optional name for the application, defaults to passed command line")
    String name;

    @CommandLine.Option(names = { "-s",
            "--session" }, description = "Optional session name for the power measurement session, defaults to epoch at the time the processing starts")
    String session;

    @CommandLine.Option(names = { "-c",
            "--command" }, required = true, description = "Command to measure energy consumption for")
    String cmd;

    PowerMeasurer measurer;
    Security security;

    public Power(PowerMeasurer measurer, Security security) throws IOException {
        this.measurer = measurer;
        this.security = security;
    }

    @Override
    public void run() {
        // start powermetrics
        final Cancellable powermetrics;
        try {
            powermetrics = measurer.startTrackingApp("kernel task", 1, "ignore");
        } catch (Exception e) {
            Log.error("Error starting kernel task", e);
            throw new RuntimeException(e);
        }

        // run target command to completion
        final var cmdPid = Uni.createFrom()
                .item(this::runCommandToMeasureToCompletion)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .await()
                .indefinitely();

        final var measure = extractPowerConsumption(cmdPid);
        Log.infof("Power consumption: %s", measure);

        powermetrics.cancel();
        measurer.stop();
        Quarkus.waitForExit();
    }

    private Measure extractPowerConsumption(long pid) {
        // first read metadata
        final var metadata = measurer.metadata();
        Log.infof("Metadata:\n%s", metadata);

        // create a synthetic component to get the total power
        final var totalPower = new TotalSyntheticComponent(metadata, SensorUnit.W, 0, 1, 2);

        final var power = measurer.persistence()
                .synthesizeAndAggregateForSession(name, session, m -> totalPower.synthesizeFrom(m.components, m.startTime))
                .map(measure -> new Measure(measure, totalPower.metadata().unit()))
                .orElseThrow(() -> new RuntimeException("Could not extract power consumption"));

        Quarkus.asyncExit();
        return power;
    }

    private Process runCommandToMeasure() {
        final var command = new ProcessBuilder().command("/bin/bash", "-c", stripped(cmd).orElseThrow());
        try {
            final var process = command.start();
            name = name == null ? String.join(" ", cmd) : name;
            session = session == null ? "" + System.currentTimeMillis() : session;
            Log.infof("Recording energy consumption for application '%s' (pid: %d) with session '%s'", name, process.pid(),
                    session);
            measurer.startTrackingApp(name, process.pid(), session);
            return process;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long runCommandToMeasureToCompletion() {
        final var process = runCommandToMeasure();
        try {
            process.waitFor();
            security.onNonZeroExitCode(process,
                    (exitCode, errorMsg) -> Log.errorf("Couldn't run command %s, got exit code: %d, reason: %s",
                            process.info().commandLine(), exitCode, errorMsg));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return process.pid();
    }

    private static Optional<String> stripped(String s) {
        return Optional.ofNullable(s)
                .map(String::strip)
                .filter(s2 -> !s2.isBlank());
    }
}
