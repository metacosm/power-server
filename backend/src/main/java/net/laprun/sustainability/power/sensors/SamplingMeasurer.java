package net.laprun.sustainability.power.sensors;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.tuples.Tuple2;
import net.laprun.sustainability.power.ProcessUtils;
import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.persistence.Persistence;
import net.laprun.sustainability.power.sensors.cpu.CPUShare;

@ApplicationScoped
public class SamplingMeasurer {

    public static final String DEFAULT_SAMPLING_PERIOD = "PT1S";
    @Inject
    PowerSensor sensor;

    @Inject
    Persistence persistence;

    @ConfigProperty(name = "power-server.sampling-period", defaultValue = DEFAULT_SAMPLING_PERIOD)
    Duration samplingPeriod;

    private Multi<Measures> periodicSensorCheck;
    private final Map<Long, Cancellable> manuallyTrackedProcesses = new ConcurrentHashMap<>();

    public PowerSensor sensor() {
        return sensor;
    }

    public Multi<SensorMeasure> stream(String pid) throws Exception {
        final var parsedPID = validPIDOrFail(pid);
        return uncheckedStream(parsedPID);
    }

    public Multi<SensorMeasure> uncheckedStream(long pid) throws Exception {
        final var registeredPID = track(pid);
        return periodicSensorCheck.map(measures -> measures.getOrDefault(registeredPID));
    }

    @SuppressWarnings("UnusedReturnValue")
    public Cancellable startTrackingApp(String appName, long pid, String session) throws Exception {
        final var tracked = uncheckedStream(pid)
                .filter(m -> SensorMeasure.missing != m)
                .subscribe()
                .with(m -> persistence.save(m, appName, session));
        manuallyTrackedProcesses.put(pid, tracked);
        return tracked;
    }

    RegisteredPID track(long pid) throws Exception {
        final var registeredPID = sensor.register(pid);

        if (!sensor.isStarted()) {
            final var adjusted = sensor.adjustSamplingPeriodIfNeeded(samplingPeriod.toMillis());
            Log.infof("%s sensor adjusted its sampling period to %dms", sensor.getClass().getSimpleName(), adjusted);
            sensor.start();

            final var samplingTicks = Multi.createFrom().ticks().every(samplingPeriod);

            if (sensor.wantsCPUShareSamplingEnabled()) {
                final var overSamplingFactor = 3;
                final var cpuSharesMulti = Multi.createFrom().ticks()
                        // over sample but over a shorter period to ensure we have an average that covers most of the sampling period
                        .every(samplingPeriod.minus(50, ChronoUnit.MILLIS).dividedBy(overSamplingFactor))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .map(tick -> CPUShare.cpuSharesFor(sensor.getRegisteredPIDs()))
                        .group()
                        .intoLists()
                        .of(overSamplingFactor)
                        .map(cpuShares -> {
                            // first convert list of mappings pid -> cpu to mapping pid -> list of cpu shares
                            Map<String, List<Double>> pidToRecordedCPUShares = new HashMap<>();
                            cpuShares.forEach(cpuShare -> cpuShare.forEach(
                                    (p, cpu) -> {
                                        if (cpu != null && cpu > 0) { // drop null values to avoid skewing average even more
                                            pidToRecordedCPUShares.computeIfAbsent(p, unused -> new ArrayList<>()).add(cpu);
                                        }
                                    }));
                            // then reduce each cpu shares list to their average
                            Map<String, Double> averages = new HashMap<>(pidToRecordedCPUShares.size());
                            pidToRecordedCPUShares.forEach((p, values) -> averages.put(p,
                                    values.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
                            return averages;
                        });
                periodicSensorCheck = Multi.createBy()
                        .combining()
                        .streams(samplingTicks, cpuSharesMulti)
                        .asTuple()
                        .log()
                        .map(this::updateSensor);
            } else {
                periodicSensorCheck = samplingTicks
                        .map(tick -> sensor.update(tick, Map.of()));
            }

            periodicSensorCheck = periodicSensorCheck
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        }

        periodicSensorCheck = periodicSensorCheck.onCancellation().invoke(() -> sensor.unregister(registeredPID));
        return registeredPID;
    }

    private Measures updateSensor(Tuple2<Long, Map<String, Double>> tuple) {
        return sensor.update(tuple.getItem1(), tuple.getItem2());
    }

    /**
     * Starts measuring even in the absence of registered PID. This will record the system's total energy consumption.
     *
     * @throws Exception if an exception occurred while measuring the energy consumption
     */
    public void start(String session) throws Exception {
        startTrackingApp(Persistence.SYSTEM_TOTAL_APP_NAME, RegisteredPID.SYSTEM_TOTAL_PID, session);
    }

    public long validPIDOrFail(String pid) {
        return ProcessUtils.validPIDOrFail(pid);
    }

    public SensorMetadata metadata() {
        return sensor.metadata();
    }

    public Duration samplingPeriod() {
        return samplingPeriod;
    }

    public Persistence persistence() {
        return persistence;
    }

    public void stop() {
        sensor.stop();
        manuallyTrackedProcesses.values().forEach(Cancellable::cancel);
    }

    public void stopTrackingProcess(long processId) {
        sensor.unregister(RegisteredPID.create(processId));
        // cancel associated process tracking
        manuallyTrackedProcesses.remove(processId).cancel();
    }
}
