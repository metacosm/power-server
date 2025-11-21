package net.laprun.sustainability.power.sensors;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.tuples.Tuple2;
import net.laprun.sustainability.power.ProcessUtils;
import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.measures.ExternalCPUShareSensorMeasure;
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

    private Multi<Tuple2<Measures, Map<String, Double>>> periodicSensorCheck;
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
        return periodicSensorCheck.map(combined -> withExternalCPUShareIfAvailable(registeredPID, combined));
    }

    private SensorMeasure withExternalCPUShareIfAvailable(RegisteredPID pid, Tuple2<Measures, Map<String, Double>> tuple) {
        final var measure = tuple.getItem1().getOrDefault(pid);
        final var cpuShares = tuple.getItem2();
        if (!cpuShares.isEmpty()) {
            final var cpuShare = cpuShares.get(pid.pidAsString());
            if (cpuShare != null && cpuShare > 0) {
                return new ExternalCPUShareSensorMeasure(measure, cpuShare);
            }
        }
        return measure;
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

        startSamplingIfNeeded();

        periodicSensorCheck = periodicSensorCheck.onCancellation().invoke(() -> sensor.unregister(registeredPID));
        return registeredPID;
    }

    private void startSamplingIfNeeded() throws Exception {
        if (!sensor.isStarted()) {
            // check if sensor wants a different sampling period
            final var samplingPeriodMillis = samplingPeriod.toMillis();
            final var adjusted = sensor.adjustSamplingPeriodIfNeeded(samplingPeriodMillis);
            if (adjusted != samplingPeriodMillis) {
                Log.infof("%s sensor adjusted its sampling period to %dms", sensor.getClass().getSimpleName(), adjusted);
            }

            // start sensor
            sensor.start();

            // manage external CPU share sampling
            final var overSamplingFactor = 3;
            final Multi<Map<String, Double>> cpuSharesMulti;
            final var cpuSharesTicks = Multi.createFrom().ticks()
                    // over sample but over a shorter period to ensure we have an average that covers most of the sampling period
                    .every(samplingPeriod.minus(50, ChronoUnit.MILLIS).dividedBy(overSamplingFactor))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
            if (sensor.wantsCPUShareSamplingEnabled()) {
                // if enabled, record a cpu share for each tick, group by the over sampling factor and average over these aggregates to produce one value for the power measure interval
                cpuSharesMulti = cpuSharesTicks
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
            } else {
                // otherwise, only emit an empty map to signify no external cpu share was recorded
                cpuSharesMulti = cpuSharesTicks.map(unused -> Map.of());
            }

            // manage periodic power sampling, measuring sensor values over the sampling period
            final var sensorSamplerMulti = Multi.createFrom().ticks()
                    .every(samplingPeriod)
                    .map(sensor::update)
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());

            // combine both multis
            periodicSensorCheck = Multi.createBy()
                    .combining()
                    .streams(sensorSamplerMulti, cpuSharesMulti)
                    .asTuple();

        }
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

    @SuppressWarnings("unused")
    public void stopTrackingProcess(long processId) {
        sensor.unregister(RegisteredPID.create(processId));
        // cancel associated process tracking
        manuallyTrackedProcesses.remove(processId).cancel();
    }
}
