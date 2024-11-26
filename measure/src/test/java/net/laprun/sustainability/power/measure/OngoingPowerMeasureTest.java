package net.laprun.sustainability.power.measure;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasureTest {
    private final static SensorMetadata metadata = new SensorMetadata(Map.of(), null, new int[0]) {

        @Override
        public int componentCardinality() {
            return 3;
        }
    };

    @Test
    void testStatistics() {
        final var m1c1 = 10.0;
        final var m1c2 = 12.0;
        final var m1c3 = 0.0;
        final var m1total = m1c1 + m1c2 + m1c3;
        final var m2c1 = 8.0;
        final var m2c2 = 17.0;
        final var m2c3 = 0.0;
        final var m2total = m2c1 + m2c2 + m2c3;
        final var m3c1 = 5.0;
        final var m3c2 = 5.0;
        final var m3c3 = 0.0;
        final var m3total = m3c1 + m3c2 + m3c3;

        final var measure = new OngoingPowerMeasure(metadata);

        final var components = new double[metadata.componentCardinality()];
        components[0] = m1c1;
        components[1] = m1c2;
        components[2] = m1c3;
        measure.recordMeasure(components);

        components[0] = m2c1;
        components[1] = m2c2;
        components[2] = m2c3;
        measure.recordMeasure(components);

        components[0] = m3c1;
        components[1] = m3c2;
        components[2] = m3c3;
        measure.recordMeasure(components);

        assertArrayEquals(new double[] { m1c1, m2c1, m3c1 }, measure.getMeasuresFor(0));
        assertArrayEquals(new double[] { m1c2, m2c2, m3c2 }, measure.getMeasuresFor(1));
        assertArrayEquals(new double[] { m1c3, m2c3, m3c3 }, measure.getMeasuresFor(2));

        assertEquals(m1c1 + m1c2 + m1c3 + m2c1 + m2c2 + m2c3 + m3c1 + m3c2 + m3c3, measure.total());
        assertEquals((m1c1 + m1c2 + m1c3 + m2c1 + m2c2 + m2c3 + m3c1 + m3c2 + m3c3) / 3, measure.average());
        assertEquals(Stream.of(m1total, m2total, m3total).min(Double::compareTo).orElseThrow(), measure.minMeasuredTotal());
        assertEquals(Stream.of(m1total, m2total, m3total).max(Double::compareTo).orElseThrow(), measure.maxMeasuredTotal());
        final var c1Avg = measure.averagesPerComponent()[0];
        final var c2Avg = measure.averagesPerComponent()[1];
        final var c3Avg = measure.averagesPerComponent()[2];
        assertEquals((m1c1 + m2c1 + m3c1) / 3, c1Avg);
        assertEquals((m1c2 + m2c2 + m3c2) / 3, c2Avg);
        assertEquals(0, c3Avg);

        final var stdVarForC1 = Math
                .sqrt((Math.pow(m1c1 - c1Avg, 2) + Math.pow(m2c1 - c1Avg, 2) + Math.pow(m3c1 - c1Avg, 2)) / (3 - 1));
        final var stdVarForC2 = Math
                .sqrt((Math.pow(m1c2 - c2Avg, 2) + Math.pow(m2c2 - c2Avg, 2) + Math.pow(m3c2 - c2Avg, 2)) / (3 - 1));

        assertEquals(stdVarForC1, measure.standardDeviations().perComponent()[0], 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(stdVarForC2, measure.standardDeviations().perComponent()[1], 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(0, measure.standardDeviations().perComponent()[2], 0.0001,
                "Standard Deviation did not match the expected value");
    }
}
