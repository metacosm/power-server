package net.laprun.sustainability.power.sensors;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import net.laprun.sustainability.power.SensorMeasure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.persistence.Persistence;

@ApplicationScoped
public class PowerMeasurer {

    public static final String DEFAULT_SAMPLING_PERIOD = "PT0.5S";
    @Inject
    PowerSensor sensor;

    @Inject
    Persistence persistence;

    @ConfigProperty(name = "net.laprun.sustainability.power.sampling-period", defaultValue = DEFAULT_SAMPLING_PERIOD)
    Duration samplingPeriod;

    private Multi<Measures> periodicSensorCheck;
    private final Map<Long, Cancellable> manuallyTrackedProcesses = new ConcurrentHashMap<>();

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
        if (!sensor.isStarted()) {
            sensor.start(samplingPeriod.toMillis());
            periodicSensorCheck = Multi.createFrom().ticks()
                    .every(samplingPeriod)
                    .map(sensor::update)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        }
        final var registeredPID = sensor.register(pid);
        // todo: the timing of things could make it so that the pid has been removed before the map operation occurs so
        //  currently return -1 instead of null but this needs to be properly addressed
        periodicSensorCheck = periodicSensorCheck.onCancellation().invoke(() -> sensor.unregister(registeredPID));
        return registeredPID;
    }

    /**
     * Starts measuring even in the absence of registered PID. This will record the system's total energy consumption.
     *
     * @throws Exception
     */
    public void start(String session) throws Exception {
        startTrackingApp(Persistence.SYSTEM_TOTAL_APP_NAME, Measures.SYSTEM_TOTAL_PID, session);
    }

    public long validPIDOrFail(String pid) {
        final var parsedPID = Long.parseLong(pid);
        ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + pid));
        return parsedPID;
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
        sensor.unregister(new RegisteredPID(processId));
        // cancel associated process tracking
        manuallyTrackedProcesses.remove(processId).cancel();
    }
}
