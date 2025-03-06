package net.laprun.sustainability.power;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import net.laprun.sustainability.power.persistence.Persistence;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.PowerSensor;
import net.laprun.sustainability.power.sensors.RegisteredPID;

@ApplicationScoped
public class PowerMeasurer {

    public static final String DEFAULT_SAMPLING_PERIOD = "PT0.5S";
    @Inject
    PowerSensor sensor;

    @ConfigProperty(name = "net.laprun.sustainability.power.sampling-period", defaultValue = DEFAULT_SAMPLING_PERIOD)
    Duration samplingPeriod;

    private Multi<Measures> periodicSensorCheck;

    public Multi<SensorMeasure> stream(String pid) throws Exception {
        final var registeredPID = track(pid);
        return periodicSensorCheck.map(measures -> measures.getOrDefault(registeredPID));
    }

    public void startTrackingApp(String appName, String pid) throws Exception {
        stream(pid).subscribe().with(m -> Persistence.save(m, appName));
    }

    private RegisteredPID track(String pid) throws Exception {
        // first make sure that the process with that pid exists
        final var parsedPID = validPIDOrFail(pid);

        if (!sensor.isStarted()) {
            sensor.start(samplingPeriod.toMillis());
            periodicSensorCheck = Multi.createFrom().ticks()
                    .every(samplingPeriod)
                    .map(sensor::update)
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        }
        final var registeredPID = sensor.register(parsedPID);
        // todo: the timing of things could make it so that the pid has been removed before the map operation occurs so
        //  currently return -1 instead of null but this needs to be properly addressed
        periodicSensorCheck = periodicSensorCheck.onCancellation().invoke(() -> sensor.unregister(registeredPID));
        return registeredPID;
    }

    protected long validPIDOrFail(String pid) {
        final var parsedPID = Long.parseLong(pid);
        ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + pid));
        return parsedPID;
    }

    public SensorMetadata metadata() {
        return sensor.metadata();
    }

    public Duration getSamplingPeriod() {
        return samplingPeriod;
    }
}
