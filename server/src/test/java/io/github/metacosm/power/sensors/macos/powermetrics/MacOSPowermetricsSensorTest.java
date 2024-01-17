package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

      /*  metadata = loadMetadata("sonoma-intel.txt");
        assertEquals(2, metadata.componentCardinality());
        checkComponent(metadata, "Package", 0);
        checkComponent(metadata, "cpuShare", 1);*/
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
   /* @Test
    void extractPowerMeasureForIntel() {
        checkPowerMeasure("sonoma-intel.txt", 8530, MacOSPowermetricsSensor.PACKAGE);
    }*/

    private static void checkPowerMeasure(String testFileName, int totalMilliWatts, String totalMeasureName) {
        var in = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFileName);
        final var sensor = new MacOSPowermetricsSensor(in);
        final var metadata = sensor.metadata();
        final var pid1 = sensor.register(29419);
        final var pid2 = sensor.register(391);

        // re-open the stream to read the measure this time
        in = Thread.currentThread().getContextClassLoader().getResourceAsStream(testFileName);
        final var measure = sensor.extractPowerMeasure(in, 0L);
        final var cpuIndex = metadata.metadataFor(totalMeasureName).index();
        final var pid1CPUShare = 23.88 / 1222.65;
        assertEquals((pid1CPUShare * totalMilliWatts), measure.getOrDefault(pid1).components()[cpuIndex]);
        final var pid2CPUShare = 283.25 / 1222.65;
        assertEquals((pid2CPUShare * totalMilliWatts), measure.getOrDefault(pid2).components()[cpuIndex]);
        // check cpu share
        final var cpuShareIndex = metadata.metadataFor(MacOSPowermetricsSensor.CPU_SHARE).index();
        assertEquals(pid1CPUShare, measure.getOrDefault(pid1).components()[cpuShareIndex]);
        assertEquals(pid2CPUShare, measure.getOrDefault(pid2).components()[cpuShareIndex]);
        // check that gpu should be 0
        final var gpuIndex = metadata.metadataFor(MacOSPowermetricsSensor.GPU).index();
        assertEquals(0.0, measure.getOrDefault(pid1).components()[gpuIndex]);
        assertEquals(0.0, measure.getOrDefault(pid2).components()[gpuIndex]);
    }
}
