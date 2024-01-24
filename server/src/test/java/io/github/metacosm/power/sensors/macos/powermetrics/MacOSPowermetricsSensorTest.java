package io.github.metacosm.power.sensors.macos.powermetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import io.github.metacosm.power.SensorMetadata;
import io.github.metacosm.power.sensors.Measures;
import io.github.metacosm.power.sensors.RegisteredPID;

class MacOSPowermetricsSensorTest {

    @Test
    void checkMetadata() throws IOException {
        var metadata = loadMetadata("sonoma-m1max.txt");
        assertEquals(4, metadata.componentCardinality());
        checkComponent(metadata, "CPU", 0);
        checkComponent(metadata, "GPU", 1);
        checkComponent(metadata, "ANE", 2);
        checkComponent(metadata, "cpuShare", 3);

        metadata = loadMetadata("monterey-m2.txt");
        assertEquals(7, metadata.componentCardinality());
        checkComponent(metadata, "CPU", 0);
        checkComponent(metadata, "GPU", 1);
        checkComponent(metadata, "ANE", 2);
        checkComponent(metadata, "cpuShare", 3);
        checkComponent(metadata, "DRAM", 4);
        checkComponent(metadata, "DCS", 5);
        checkComponent(metadata, "Package", 6);

        metadata = loadMetadata("sonoma-intel.txt");
        assertEquals(2, metadata.componentCardinality());
        checkComponent(metadata, "Package", 0);
        checkComponent(metadata, "cpuShare", 1);
    }

    private static SensorMetadata loadMetadata(String fileName) throws IOException {
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            return loadMetadata(in);
        }
    }

    private static SensorMetadata loadMetadata(InputStream in) {
        final var sensor = new MacOSPowermetricsSensor(in);
        return sensor.metadata();
    }

    private static void checkComponent(SensorMetadata metadata, String name, int index) {
        // check string instead of constants to ensure "API" compatibility as these keys will be published
        final var component = metadata.metadataFor(name);
        assertEquals(name, component.name());
        assertEquals(index, component.index());
    }

    @Test
    void extractPowerMeasureForM1Max() {
        checkPowerMeasure("sonoma-m1max.txt", 211, MacOSPowermetricsSensor.CPU);
    }

    @Test
    void extractPowerMeasureForM2() {
        checkPowerMeasure("monterey-m2.txt", 10, MacOSPowermetricsSensor.CPU);
    }

    @Test
    void extractPowerMeasureForIntel() {
        checkPowerMeasure("sonoma-intel.txt", 8.53f, MacOSPowermetricsSensor.PACKAGE);
    }

    private static void checkPowerMeasure(String testFileName, float total, String totalMeasureName) {
        var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFileName);
        final var sensor = new MacOSPowermetricsSensor(in);
        final var metadata = sensor.metadata();
        final var pid1 = sensor.register(29419);
        final var pid2 = sensor.register(391);

        // re-open the stream to read the measure this time
        in = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFileName);
        final var measure = sensor.extractPowerMeasure(in, 0L);
        final var totalMeasureMetadata = metadata.metadataFor(totalMeasureName);
        final var pid1CPUShare = 23.88 / 1222.65;
        assertEquals((pid1CPUShare * total), getComponent(measure, pid1, totalMeasureMetadata));
        final var pid2CPUShare = 283.25 / 1222.65;
        assertEquals((pid2CPUShare * total), getComponent(measure, pid2, totalMeasureMetadata));
        // check cpu share
        final var cpuShareMetadata = metadata.metadataFor(MacOSPowermetricsSensor.CPU_SHARE);
        assertEquals(pid1CPUShare, getComponent(measure, pid1, cpuShareMetadata));
        assertEquals(pid2CPUShare, getComponent(measure, pid2, cpuShareMetadata));
        if (metadata.exists(MacOSPowermetricsSensor.GPU)) {
            // check that gpu should be 0
            final var gpuMetadata = metadata.metadataFor(MacOSPowermetricsSensor.GPU);
            assertEquals(0.0, getComponent(measure, pid1, gpuMetadata));
            assertEquals(0.0, getComponent(measure, pid2, gpuMetadata));
        }
    }

    private static double getComponent(Measures measure, RegisteredPID pid1, SensorMetadata.ComponentMetadata metadata) {
        final var index = metadata.index();
        final boolean isInWatt = metadata.unit().equals("W");
        return measure.getOrDefault(pid1).components()[index];
    }
}
