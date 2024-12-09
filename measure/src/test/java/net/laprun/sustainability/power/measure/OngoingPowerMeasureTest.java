package net.laprun.sustainability.power.measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;

public class OngoingPowerMeasureTest {
    private final static SensorMetadata metadata = new SensorMetadata(List.of(), null) {

        @Override
        public int componentCardinality() {
            return 3;
        }
    };

    @Test
    void testBasics() {
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

        assertThat(measure.getMeasuresFor(0)).hasValue(new double[] { m1c1, m2c1, m3c1 });
        assertThat(measure.getMeasuresFor(1)).hasValue(new double[] { m1c2, m2c2, m3c2 });
        assertThat(measure.getMeasuresFor(2)).isEmpty();

        assertEquals(m1c1 + m1c2 + m1c3 + m2c1 + m2c2 + m2c3 + m3c1 + m3c2 + m3c3, measure.total());
        assertEquals(Stream.of(m1total, m2total, m3total).min(Double::compareTo).orElseThrow(), measure.minMeasuredTotal());
        assertEquals(Stream.of(m1total, m2total, m3total).max(Double::compareTo).orElseThrow(), measure.maxMeasuredTotal());
    }
}
