package io.github.metacosm.power;

import io.github.metacosm.power.sensors.PowerSensor;
import io.github.metacosm.power.sensors.RegisteredPID;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class PowerMeasurer {

    public static final int SAMPLING_FREQUENCY_IN_MILLIS = 500;
    @Inject
    PowerSensor sensor;
    
    private Multi<Map<RegisteredPID, double[]>> stream;

    public Multi<double[]> startTracking(String pid) throws Exception {
        // first make sure that the process with that pid exists
        final var parsedPID = Long.parseLong(pid);
        ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + pid));

        if(!sensor.isStarted()) {
            sensor.start(SAMPLING_FREQUENCY_IN_MILLIS);
            stream = Multi.createFrom().ticks().every(Duration.ofMillis(SAMPLING_FREQUENCY_IN_MILLIS)).map(sensor::update);
        }
        final var registeredPID = sensor.register(parsedPID);
        return stream.map(registeredPIDMap -> registeredPIDMap.get(registeredPID))
                .onCancellation().invoke(() -> sensor.unregister(registeredPID));
    }
}
