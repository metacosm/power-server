package net.laprun.sustainability.power.sensors.linux.rapl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProcessReadRAPLFileTest {
    @Test
    void shouldExtractEnergyInMicroJoules() {
        ProcessReadRAPLFile reader = new ProcessReadRAPLFile(
                ResourceHelper.getResourcePath(getClass(), "rapl/intel-rapl_1/energy_uj"));
        assertEquals(12345, reader.extractEnergyInMicroJoules());
    }

    @Test
    void shouldExtractName() {
        ProcessReadRAPLFile reader = new ProcessReadRAPLFile(
                ResourceHelper.getResourcePath(getClass(), "rapl/intel-rapl_1/name"));
        assertEquals("CPU", reader.contentAsString());
    }
}
