package io.github.metacosm.power.sensors.macos.powermetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MacOSPowermetricsSensorTest {

    @Test
    void checkMetadata() {
        final var sensor = new MacOSPowermetricsSensor();
        final var metadata = sensor.metadata();
        assertEquals("cpu", metadata.metadataFor(MacOSPowermetricsSensor.CPU).name());
    }

    @Test
    void extractPowerMeasure() {
        final var sensor = new MacOSPowermetricsSensor();
        final var pid1 = sensor.register(29419);
        final var pid2 = sensor.register(391);
        final var measure = sensor
                .extractPowerMeasure(Thread.currentThread().getContextClassLoader().getResourceAsStream("sonoma-m1max.txt"));
        final var metadata = sensor.metadata();
        assertEquals(((23.88 / 1222.65) * 211), measure.get(pid1)[metadata.metadataFor(MacOSPowermetricsSensor.CPU).index()]);
        assertEquals(((283.25 / 1222.65) * 211), measure.get(pid2)[metadata.metadataFor(MacOSPowermetricsSensor.CPU).index()]);
    }
}
