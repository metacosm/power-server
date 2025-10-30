package net.laprun.sustainability.power;

import io.quarkus.test.Mock;
import net.laprun.sustainability.power.sensors.SamplingMeasurer;

@Mock
@SuppressWarnings("unused")
public class MockSamplingMeasurer extends SamplingMeasurer {

    @Override
    public long validPIDOrFail(String pid) {
        return Long.parseLong(pid);
    }
}
