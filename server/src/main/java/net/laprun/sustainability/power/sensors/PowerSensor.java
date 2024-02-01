package net.laprun.sustainability.power.sensors;

import net.laprun.sustainability.power.SensorMetadata;

public interface PowerSensor {

    default void stop() {
    }

    SensorMetadata metadata();

    boolean isStarted();

    void start(long samplingFrequencyInMillis) throws Exception;

    RegisteredPID register(long pid);

    Measures update(Long tick);

    void unregister(RegisteredPID registeredPID);
}
