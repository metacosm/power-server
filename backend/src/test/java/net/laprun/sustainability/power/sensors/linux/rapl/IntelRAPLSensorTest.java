package net.laprun.sustainability.power.sensors.linux.rapl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;

public class IntelRAPLSensorTest {
    @Test
    void checkMetadata() {
        var metadata = loadMetadata("rapl/intel-rapl_1/energy_uj", "rapl/intel-rapl_2/energy_uj");
        assertEquals(4, metadata.componentCardinality());
        checkComponent(metadata, "CPU", 0);
        checkComponent(metadata, "GPU", 1);
        checkComponent(metadata, "CPU_uj", 2);
        checkComponent(metadata, "GPU_uj", 3);
    }

    @BeforeAll
    static void beforeAll() {
        TestMode.enabled = true;
    }

    @AfterAll
    static void afterAll() {
        TestMode.enabled = false;
    }

    static class TestRAPLFile implements RAPLFile {
        private final long[] values;
        private int callCount = 0;
        private final long[] measureTimes;

        public TestRAPLFile(long... values) {
            this.values = values;
            this.measureTimes = new long[values.length];
        }

        public long valueAt(int index) {
            return this.values[index];
        }

        public long measureTimeFor(int index) {
            return this.measureTimes[index];
        }

        public int callCount() {
            return this.callCount;
        }

        @Override
        public long extractEnergyInMicroJoules() {
            return values[callCount++];
        }

        void recordMeasureTime(long time) {
            // this is called after the extract method, which increases the call count, is called but we need to record the time for the same call, so locally decrease call count
            measureTimes[callCount - 1] = time;
        }

        @Override
        public String contentAsString() {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void basicWattComputationShouldWork() {
        final var power = IntelRAPLSensor.computePowerInMilliWatts(200, 100, 2000, 1000);
        assertEquals((double) 100 / 1000 / 1000, power);
    }

    private static class TestIntelRAPLSensor extends IntelRAPLSensor {
        TestIntelRAPLSensor(SortedMap<String, RAPLFile> files) {
            super(files);
        }

        @Override
        protected void readAndRecordSensor(BiConsumer<Long, Integer> onReadingSensorValueAtIndex, long newUpdateStartEpoch) {
            // also record measure time so that we can run tests with proper time recording behavior instead of fixing times
            BiConsumer<Long, Integer> consumer = (measure, index) -> ((TestRAPLFile) raplFile(index))
                    .recordMeasureTime(newUpdateStartEpoch);
            if (onReadingSensorValueAtIndex != null) {
                consumer = consumer.andThen(onReadingSensorValueAtIndex);
            }
            super.readAndRecordSensor(consumer, newUpdateStartEpoch);
        }
    }

    @Test
    void wattComputationShouldWork() throws Exception {
        final var raplFile = new TestRAPLFile(10000L, 20000L, 30000L);
        final var sensor = new TestIntelRAPLSensor(new TreeMap<>(Map.of("sensor", raplFile)));
        sensor.start(500);
        final var pid = sensor.register(1234L);
        final var measures = sensor.update(1L);
        final var components = measures.getOrDefault(pid).components();
        assertEquals(1, components.length);
        assertEquals(2, raplFile.callCount());
        final var interval = raplFile.measureTimeFor(1) - raplFile.measureTimeFor(0);
        final var expected = (double) (raplFile.valueAt(1) - raplFile.valueAt(0)) / interval / 1000;
        assertEquals(expected, components[0]);
    }

    private SensorMetadata loadMetadata(String... fileNames) {
        Class<? extends IntelRAPLSensorTest> clazz = getClass();
        final var files = Arrays.stream(fileNames)
                .map(name -> ResourceHelper.getResourcePathAsString(clazz, name))
                .toArray(String[]::new);
        return new IntelRAPLSensor(files).metadata();
    }

    private static void checkComponent(SensorMetadata metadata, String name, int index) {
        // check string instead of constants to ensure "API" compatibility as these keys will be published
        final var component = metadata.metadataFor(name);
        assertEquals(name, component.name());
        assertEquals(index, component.index());
    }
}
