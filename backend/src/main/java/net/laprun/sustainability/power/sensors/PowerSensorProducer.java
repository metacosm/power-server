package net.laprun.sustainability.power.sensors;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import net.laprun.sustainability.power.Security;
import net.laprun.sustainability.power.sensors.linux.rapl.IntelRAPLSensor;
import net.laprun.sustainability.power.sensors.macos.powermetrics.ProcessMacOSPowermetricsSensor;

@Singleton
public class PowerSensorProducer {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    @Inject
    Security security;

    @Produces
    public PowerSensor sensor() {
        return determinePowerSensor(security);
    }

    public static PowerSensor determinePowerSensor(Security security) {
        if (OS_NAME.contains("mac os x")) {
            return new ProcessMacOSPowermetricsSensor(security);
        }

        if (!OS_NAME.contains("linux")) {
            throw new RuntimeException("Unsupported platform: " + System.getProperty("os.name"));
        }
        return new IntelRAPLSensor();
    }
}
