package net.laprun.sustainability.power.sensors.macos.powermetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.Measures;
import net.laprun.sustainability.power.sensors.RegisteredPID;

class MacOSPowermetricsSensorTest {

    @Test
    void checkMetadata() {
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

    private static SensorMetadata loadMetadata(String fileName) {
        return new ResourceMacOSPowermetricsSensor(fileName).metadata();
    }

    private static void checkComponent(SensorMetadata metadata, String name, int index) {
        // check string instead of constants to ensure "API" compatibility as these keys will be published
        final var component = metadata.metadataFor(name);
        assertEquals(name, component.name());
        assertEquals(index, component.index());
    }

    @Test
    void extractPowerMeasureForM1Max() {
        checkPowerMeasure("sonoma-m1max.txt", 211, MacOSPowermetricsSensor.CPU, 1012);
    }

    @Test
    void extractPowerMeasureForM2() {
        checkPowerMeasure("monterey-m2.txt", 10, MacOSPowermetricsSensor.CPU, 1012);
    }

    @Test
    void extractPowerMeasureForM4() {
        final var startUpdateEpoch = System.currentTimeMillis();
        final var sensor = new ResourceMacOSPowermetricsSensor("tahoe-m4-summary.txt", startUpdateEpoch);
        final var metadata = sensor.metadata();
        final var pid0 = sensor.register(2976);

        final var cpu = metadata.metadataFor(MacOSPowermetricsSensor.CPU);
        // re-open the stream to read the measure this time
        final var measure = sensor.update(0L);

        final var totalCPUPower = 420;
        final var totalCPUTime = 1287.34;
        // Process CPU power should be equal to sample ms/s divided for process (here: 116.64) by total samples (1222.65) times total CPU power
        final var pidCPUShare = 224.05 / totalCPUTime;
        assertEquals(pidCPUShare * totalCPUPower, getComponent(measure, pid0, cpu));
        assertEquals(startUpdateEpoch + 10458, measure.lastMeasuredUpdateEndEpoch());
    }

    @Test
    void checkTotalPowerMeasureEvenWhenRegisteredProcessIsNotFound() {
        final var startUpdateEpoch = System.currentTimeMillis();
        final var sensor = new ResourceMacOSPowermetricsSensor("monterey-m2.txt", startUpdateEpoch);
        final var metadata = sensor.metadata();
        sensor.register(-666);

        // re-open the stream to read the measure this time
        final var measure = sensor.update(0L);

        assertEquals(0, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.ANE));
        assertEquals(19, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.DRAM));
        assertEquals(36, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.DCS));
        assertEquals(10, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.CPU));
        assertEquals(0, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.GPU));
        assertEquals(25, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.PACKAGE));
        assertEquals(1.0, getTotalSystemComponent(measure, metadata, MacOSPowermetricsSensor.CPU_SHARE));
        assertEquals(startUpdateEpoch + 1012, measure.lastMeasuredUpdateEndEpoch());
    }

    @Test
    void extractPowerMeasureForIntel() {
        checkPowerMeasure("sonoma-intel.txt", 8.53f, MacOSPowermetricsSensor.PACKAGE, 1002);
    }

    @Test
    void extractionShouldWorkForLowProcessIds() {
        final var sensor = new ResourceMacOSPowermetricsSensor("sonoma-m1max.txt");
        final var metadata = sensor.metadata();
        final var pid0 = sensor.register(0);
        final var pid1 = sensor.register(1);

        final var cpu = metadata.metadataFor(MacOSPowermetricsSensor.CPU);
        // re-open the stream to read the measure this time
        final var measure = sensor.update(0L);
        // Process CPU power should be equal to sample ms/s divided for process (here: 116.64) by total samples (1222.65) times total CPU power
        var pidCPUShare = 116.64 / 1222.65;
        assertEquals(pidCPUShare * 211, getComponent(measure, pid0, cpu));

        pidCPUShare = 7.90 / 1222.65;
        assertEquals(pidCPUShare * 211, getComponent(measure, pid1, cpu));
    }

    private static void checkPowerMeasure(String testFileName, float total, String totalMeasureName,
            long expectedMeasureDuration) {
        final var startUpdateEpoch = System.currentTimeMillis();
        final var sensor = new ResourceMacOSPowermetricsSensor(testFileName, startUpdateEpoch);
        final var metadata = sensor.metadata();
        final var pid1 = sensor.register(29419);
        final var pid2 = sensor.register(391);

        // re-open the stream to read the measure this time
        final var measure = sensor.update(0L);
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
        assertEquals(startUpdateEpoch + expectedMeasureDuration, measure.lastMeasuredUpdateEndEpoch());
    }

    private static double getComponent(Measures measure, RegisteredPID pid1, SensorMetadata.ComponentMetadata metadata) {
        final var index = metadata.index();
        return measure.getOrDefault(pid1).components()[index];
    }

    private static double getTotalSystemComponent(Measures measure, SensorMetadata metadata, String componentName) {
        return getComponent(measure, RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID, metadata.metadataFor(componentName));
    }
}
