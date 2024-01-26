package io.github.metacosm.power;

import io.github.metacosm.power.sensors.macos.powermetrics.ResourceMacOSPowermetricsSensor;
import io.quarkus.test.Mock;

@Mock
public class MockPowerSensor extends ResourceMacOSPowermetricsSensor {
    public MockPowerSensor() {
        super("sonoma-m1max.txt");
    }
}
