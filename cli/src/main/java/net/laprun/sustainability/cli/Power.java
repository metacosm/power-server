package net.laprun.sustainability.cli;

import static net.laprun.sustainability.power.sensors.PowerSensor.EXTERNAL_CPU_SHARE_COMPONENT_NAME;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import net.laprun.sustainability.power.Measure;
import net.laprun.sustainability.power.ProcessUtils;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.total.Totaler;
import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;
import net.laprun.sustainability.power.persistence.Persistence;
import net.laprun.sustainability.power.sensors.SamplingMeasurer;
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

    private final SamplingMeasurer measurer;
    private final Totaler totaler;

    public Power(SamplingMeasurer measurer) {
        this.measurer = measurer;
        final var sensor = measurer.sensor();
        if (!sensor.supportsProcessAttribution()) {
            sensor.enableCPUShareSampling(true);
        }
        final var metadata = measurer.metadata();
        totaler = new Totaler(metadata, SensorUnit.W);
    }

    @Override
    public void run() {
        if (cmd == null || cmd.isEmpty()) {
            Log.info("No command specified, exiting.");
            return;
        }

        name = name == null ? String.join(" ", cmd) : name;
        session = session == null ? Persistence.defaultSession(name) : session;

        try {
            measurer.start(session);

            final var commandHandler = new ExternalProcessHandler(cmd);
            final var command = new NuProcessBuilder(commandHandler, commandHandler.command());
            final var process = command.start();

            final var pid = process.getPID();
            Log.infof("Recording energy consumption for application '%s' (pid: %d) with session '%s'", name, pid,
                    session);
            measurer.startTrackingApp(name, pid, session);

            process.waitFor(60, TimeUnit.SECONDS);

            final var measureTime = measurer.persistence()
                    .synthesizeAndAggregateForSession(Persistence.SYSTEM_TOTAL_APP_NAME, session, m -> (double) m.duration())
                    .orElseThrow(() -> new RuntimeException("Could not compute measure duration"));
            final var systemPower = extractPowerConsumption(Persistence.SYSTEM_TOTAL_APP_NAME, false);
            Log.infof("Command ran for: %dms, measure time: %3fms", commandHandler.duration(), measureTime);
            Log.infof("Total system power consumption: %3.2f%s", systemPower.value(), systemPower.unit());
            if (totaler.isAttributed()) {
                final var appPower = extractPowerConsumption(name, false);
                Log.infof("App '%s' power consumption: %3.2f%s", cmd, appPower.value(), appPower.unit());
            } else {
                if (measurer.sensor().wantsCPUShareSamplingEnabled()) {
                    final var appPower = extractPowerConsumption(name, true);
                    Log.infof("App '%s' power consumption: %3.2f%s", cmd, appPower.value(), appPower.unit());
                } else {
                    Log.info(
                            "Power consumption for this platform is not currently attributed: no per-process power is currently measured");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            measurer.stop();
        }

        Quarkus.waitForExit();
    }

    private Measure extractPowerConsumption(String applicationName, boolean useExternalCPUShare) {
        int cpuShareComponent = measurer.metadata().metadataFor(EXTERNAL_CPU_SHARE_COMPONENT_NAME).index();
        final var appPower = measurer.persistence()
                .synthesizeAndAggregateForSession(applicationName, session,
                        m -> {
                            double factor = useExternalCPUShare ? m.components[cpuShareComponent] : 1.0;
                            return factor * totaler.computeTotalFrom(m.components);
                        })
                .map(measure -> new Measure(measure, totaler.expectedResultUnit()))
                .orElseThrow(() -> new RuntimeException("Could not extract power consumption"));

        Quarkus.asyncExit();
        return appPower;
    }

    private class ExternalProcessHandler extends BaseProcessHandler {
        private long startTime;
        private long endTime;

        public ExternalProcessHandler(String cmd) {
            super("/bin/sh", "-c", stripped(cmd).orElseThrow());
        }

        @Override
        public void onStart(NuProcess nuProcess) {
            super.onStart(nuProcess);
            final var start = System.currentTimeMillis();
            ProcessUtils.processHandleOf(nuProcess.getPID()).onExit()
                    .thenAccept(ph -> startTime = ph.info().startInstant().map(Instant::toEpochMilli).orElse(start));
        }

        @Override
        public void onExit(int statusCode) {
            try {
                super.onExit(statusCode);
            } finally {
                endTime = System.currentTimeMillis();
                measurer.stop();
            }
        }

        public long duration() {
            return endTime - startTime;
        }

        private static Optional<String> stripped(String s) {
            return Optional.ofNullable(s)
                    .map(String::strip)
                    .filter(s2 -> !s2.isBlank());
        }
    }
}
