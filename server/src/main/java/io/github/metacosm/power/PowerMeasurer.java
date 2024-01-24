package io.github.metacosm.power;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.PowerSensor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;

@ApplicationScoped
public class PowerMeasurer {

    public static final int SAMPLING_FREQUENCY_IN_MILLIS = 500;
    @Inject
    PowerSensor sensor;

    private Multi<Measures> periodicSensorCheck;

    public Multi<SensorMeasure> startTracking(String pid) throws Exception {
        // first make sure that the process with that pid exists
        final var parsedPID = Long.parseLong(pid);
        ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + pid));

        if (!sensor.isStarted()) {
            sensor.start(SAMPLING_FREQUENCY_IN_MILLIS);
            periodicSensorCheck = Multi.createFrom().ticks()
                    .every(Duration.ofMillis(SAMPLING_FREQUENCY_IN_MILLIS))
                    .map(sensor::update)
                    .broadcast()
                    .withCancellationAfterLastSubscriberDeparture()
                    .toAtLeast(1)
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
        }
        final var registeredPID = sensor.register(parsedPID);
        // todo: the timing of things could make it so that the pid has been removed before the map operation occurs so
        //  currently return -1 instead of null but this needs to be properly addressed
        return periodicSensorCheck
                .map(measures -> measures.getOrDefault(registeredPID))
                .onCancellation().invoke(() -> sensor.unregister(registeredPID));
    }

    public SensorMetadata metadata() {
        return sensor.metadata();
    }
}
