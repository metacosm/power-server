package net.laprun.sustainability.power.measure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasureTest {
    private final static SensorMetadata metadata = new SensorMetadata(Map.of(), null, new int[0]) {

        @Override
        public int componentCardinality() {
            return 3;
        }
    };

    static List<ComponentMeasure.Factory<?>> factories() {
        return List.of(DescriptiveStatisticsComponentMeasure.factory(2), HdrHistogramComponentMeasure::new);
    }

    @ParameterizedTest
    @MethodSource("factories")
    void testStatistics(ComponentMeasure.Factory<?> factory) {
        final var m1c1 = 10.0;
        final var m1c2 = 12.0;
        final var m1c3 = 0.0;
        final var m2c1 = 8.0;
        final var m2c2 = 17.0;
        final var m2c3 = 0.0;

        final var measure = new OngoingPowerMeasure(metadata, factory);

        final var components = new double[metadata.componentCardinality()];
        components[0] = m1c1;
        components[1] = m1c2;
        components[2] = m1c3;
        measure.recordMeasure(components);

        components[0] = m2c1;
        components[1] = m2c2;
        components[2] = m2c3;
        measure.recordMeasure(components);

        assertEquals(m1c1 + m1c2 + m2c1 + m2c2 + m1c3 + m2c3, measure.total());
        assertEquals((m1c1 + m1c2 + m2c1 + m2c2 + m1c3 + m2c3) / 2, measure.average());
        assertEquals(Math.min(m1c1 + m1c2 + m1c3, m2c1 + m2c2 + m2c3), measure.minMeasuredTotal());
        assertEquals(Math.max(m1c1 + m1c2 + m1c3, m2c1 + m2c2 + m2c3), measure.maxMeasuredTotal());
        final var c1Avg = measure.averagesPerComponent()[0];
        final var c2Avg = measure.averagesPerComponent()[1];
        final var c3Avg = measure.averagesPerComponent()[2];
        assertEquals((m1c1 + m2c1) / 2, c1Avg);
        assertEquals((m1c2 + m2c2) / 2, c2Avg);
        assertEquals(0, c3Avg);

        final var stdVarForC1 = Math.sqrt((Math.pow(m1c1 - c1Avg, 2) + Math.pow(m2c1 - c1Avg, 2)) / (2 - 1));
        final var stdVarForC2 = Math.sqrt((Math.pow(m1c2 - c2Avg, 2) + Math.pow(m2c2 - c2Avg, 2)) / (2 - 1));

        assertEquals(stdVarForC1, measure.standardDeviations().perComponent()[0], 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(stdVarForC2, measure.standardDeviations().perComponent()[1], 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(0, measure.standardDeviations().perComponent()[2], 0.0001,
                "Standard Deviation did not match the expected value");
    }
}
