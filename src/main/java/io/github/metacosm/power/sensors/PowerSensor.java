package io.github.metacosm.power.sensors;

import io.github.metacosm.power.SensorMetadata;

import java.util.Map;

public interface PowerSensor {

    default void stop() {
    }

    SensorMetadata metadata();

    boolean isStarted();

    void start(long samplingFrequencyInMillis) throws Exception;

    RegisteredPID register(long pid);

    Map<RegisteredPID,double[]> update(Long tick);

    void unregister(RegisteredPID registeredPID);
}
