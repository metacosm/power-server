package net.laprun.sustainability.power;

import io.quarkus.test.Mock;
import net.laprun.sustainability.power.sensors.macos.powermetrics.ResourceMacOSPowermetricsSensor;

@Mock
@SuppressWarnings("unused")
public class MockPowerSensor extends ResourceMacOSPowermetricsSensor {

    public MockPowerSensor() {
        super(System.getProperty("os.arch").equalsIgnoreCase("x86_64") ? "sonoma-intel.txt" : "sonoma-m1max.txt");
    }
}
