package io.github.metacosm.power;

import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PowerMeasurer {

    public static final int SAMPLING_FREQUENCY_IN_MILLIS = 500;
    @Inject
    PowerSensor sensor;
    
    private Multi<Map<RegisteredPID, double[]>> periodicSensorCheck;

    public Multi<double[]> startTracking(String pid) throws Exception {
        // first make sure that the process with that pid exists
        final var parsedPID = Long.parseLong(pid);
        ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + pid));

        if(!sensor.isStarted()) {
            sensor.start(SAMPLING_FREQUENCY_IN_MILLIS);
            periodicSensorCheck = Multi.createFrom().ticks()
                    .every(Duration.ofMillis(SAMPLING_FREQUENCY_IN_MILLIS))
                    .emitOn(Infrastructure.getDefaultWorkerPool())
                    .map(sensor::update);
        }
        final var registeredPID = sensor.register(parsedPID);
        return periodicSensorCheck.map(registeredPIDMap -> registeredPIDMap.get(registeredPID))
                .onCancellation().invoke(() -> sensor.unregister(registeredPID));
    }

    public SensorMetadata metadata() {
        return sensor.metadata();
    }
}
