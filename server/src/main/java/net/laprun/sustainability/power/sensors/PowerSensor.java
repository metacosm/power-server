package net.laprun.sustainability.power.sensors;

import net.laprun.sustainability.power.SensorMetadata;

/**
 * A representation of a power-consumption sensor.
 */
public interface PowerSensor {

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
     * @param samplingFrequencyInMillis the number of milliseconds between emitted measures
     * @throws Exception if the sensor couldn't be started for some reason
     */
    void start(long samplingFrequencyInMillis) throws Exception;

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
     * Updates the ongoing {@link Measures} being recorded by this sensor for the given tick
     * 
     * @param tick an ordinal value tracking the number of recorded measures being taken by the sensor since it started
     *        measuring power consumption
     * @return the {@link Measures} object recording the measures this sensor has taken since it started measuring
     */
    Measures update(Long tick);

    /**
     * Unregisters the specified {@link RegisteredPID} with this sensor thus signaling that clients are not interested in
     * tracking the consumption of the associated process anymore
     * 
     * @param registeredPID the {@link RegisteredPID} that was returned when the process we want to stop tracking was first
     *        registered with this sensor
     */
    void unregister(RegisteredPID registeredPID);
}
