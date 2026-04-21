package net.laprun.sustainability.power.sensors;

import java.util.Set;

import io.smallrye.mutiny.Multi;
import net.laprun.sustainability.power.SensorMetadata;

/**
 * A representation of a power-consumption sensor.
 */
public interface PowerSensor {
    /**
     * Whether the sensor supports process attribution of power, i.e. is measured power imputed to each process or does
     * attribution need to be performed externally to the sensor.
     *
     * @return {@code true} if this sensor attributes power to each individual processes, {@code false} otherwise
     */
    default boolean supportsProcessAttribution() {
        return false;
    }

    boolean wantsCPUShareSamplingEnabled();

    void enableCPUShareSampling(boolean enable);

    default long adjustSamplingPeriodIfNeeded(long requestedSamplingPeriodInMillis) {
        return requestedSamplingPeriodInMillis;
    }

    /**
     * Stops measuring power consumption
     */
    default void stop() {
    }

    /**
     * Retrieves the metadata associated with the sensor, in particular, which components are supported and how they are laid
     * out in the {@link net.laprun.sustainability.power.SensorMeasure} that the sensor outputs
     *
     * @return the metadata associated with the sensor
     */
    SensorMetadata metadata();

    /**
     * Whether the sensor has started measuring power consumption or not
     *
     * @return {@code true} if measures are ongoing, {@code false} otherwise
     */
    boolean isStarted();

    /**
     * Starts emitting power consumption measures at the given frequency
     *
     * @throws Exception if the sensor couldn't be started for some reason
     */
    Multi<Measures> start() throws Exception;

    /**
     * Registers the provided process identifier (pid) with the sensor in case it can provide per-process measures. For sensors
     * that only provide system-wide measures, this probably won't be doing much more than track which processes are of interest
     * to clients of the sensor.
     *
     * @param pid the process identifier which power consumption is supposed to be tracked
     * @return a {@link RegisteredPID} recording the tracking of the specified pid by the sensor
     */
    RegisteredPID register(long pid);

    /**
     * Unregisters the specified {@link RegisteredPID} with this sensor thus signaling that clients are not interested in
     * tracking the consumption of the associated process anymore
     *
     * @param registeredPID the {@link RegisteredPID} that was returned when the process we want to stop tracking was first
     *        registered with this sensor
     */
    void unregister(RegisteredPID registeredPID);

    Set<String> registeredPIDsAsStrings();

    Set<RegisteredPID> registeredPIDs();
}
