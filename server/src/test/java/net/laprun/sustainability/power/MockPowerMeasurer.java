package net.laprun.sustainability.power;

import io.quarkus.test.Mock;
import net.laprun.sustainability.power.sensors.PowerMeasurer;

@Mock
@SuppressWarnings("unused")
public class MockPowerMeasurer extends PowerMeasurer {

    @Override
    public long validPIDOrFail(String pid) {
        return Long.parseLong(pid);
    }
}
