package io.github.metacosm.power.sensors;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.github.metacosm.power.sensors.linux.rapl.IntelRAPLSensor;
import io.github.metacosm.power.sensors.macos.powermetrics.MacOSPowermetricsSensor;

@Singleton
public class PowerSensorProducer {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    @Produces
    public PowerSensor sensor() {
        return determinePowerSensor();
    }

    public static PowerSensor determinePowerSensor() {
        if (OS_NAME.contains("mac os x")) {
            return new MacOSPowermetricsSensor();
        }

        if (!OS_NAME.contains("linux")) {
            throw new RuntimeException("Unsupported platform: " + System.getProperty("os.name"));
        }
        return new IntelRAPLSensor();
    }
}
