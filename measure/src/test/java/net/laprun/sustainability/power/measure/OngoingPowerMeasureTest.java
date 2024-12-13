package net.laprun.sustainability.power.measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.TotalMeasureProcessor;

public class OngoingPowerMeasureTest {
    private final static SensorMetadata metadata = SensorMetadata
            .withNewComponent("cp1", null, true, "mW", true)
            .withNewComponent("cp2", null, true, "mW", true)
            .withNewComponent("cp3", null, true, "mW", true)
            .build();

    @Test
    void checkThatTotalComponentIsProperlyAdded() {
        final var metadata = SensorMetadata
                .withNewComponent("cp1", null, true, null, false)
                .withNewComponent("cp2", null, true, null, false)
                .withNewComponent("cp3", null, true, null, false)
                .build();
        var measure = new OngoingPowerMeasure(metadata);
        assertThat(measure.metadata().totalComponents()).isEmpty();
    }

    @Test
    void testBasics() {
        final var m1c1 = 10.0;
        final var m1c2 = 12.0;
        final var m1c3 = 0.0;
        final var m2c1 = 8.0;
        final var m2c2 = 17.0;
        final var m2c3 = 0.0;
        final var m3c1 = 5.0;
        final var m3c2 = 5.0;
        final var m3c3 = 0.0;

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

        var measures = measure.getNthTimestampedMeasures(0);
        assertThat(measures.measures()).isEqualTo(new double[] { m1c1, m1c2, m1c3 });
        measures = measure.getNthTimestampedMeasures(1);
        assertThat(measures.measures()).isEqualTo(new double[] { m2c1, m2c2, m2c3 });
        measures = measure.getNthTimestampedMeasures(2);
        assertThat(measures.measures()).isEqualTo(new double[] { m3c1, m3c2, m3c3 });
    }

    @Test
    void testTotal() {
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
        final var totalProc = new TotalMeasureProcessor(metadata, 0, 1, 2);
        measure.registerMeasureProcessor(totalProc);

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

        assertEquals(m1c1 + m1c2 + m1c3 + m2c1 + m2c2 + m2c3 + m3c1 + m3c2 + m3c3, totalProc.total());
        assertEquals(Stream.of(m1total, m2total, m3total).min(Double::compareTo).orElseThrow(), totalProc.minMeasuredTotal());
        assertEquals(Stream.of(m1total, m2total, m3total).max(Double::compareTo).orElseThrow(), totalProc.maxMeasuredTotal());
    }

    @Test
    void processorsShouldBeCalled() {
        final var random = Random.from(RandomGenerator.getDefault());
        final var m1c1 = random.nextDouble();
        final var m1c2 = random.nextDouble();
        final var m2c1 = random.nextDouble();
        final var m2c2 = random.nextDouble();

        final var measure = new OngoingPowerMeasure(metadata);
        final var testProc = new TestComponentProcessor();
        measure.registerProcessorFor(0, testProc);

        final var components = new double[metadata.componentCardinality()];
        components[0] = m1c1;
        components[1] = m1c2;
        measure.recordMeasure(components);

        components[0] = m2c1;
        components[1] = m2c2;
        measure.recordMeasure(components);

        final var processors = measure.processors();
        assertThat(processors.processorsFor(0)).hasSize(1);
        assertThat(processors.processorsFor(1)).isEmpty();
        assertThat(processors.processorsFor(2)).isEmpty();

        assertThat(testProc.values.getFirst().value()).isEqualTo(m1c1);
        assertThat(testProc.values.getLast().value()).isEqualTo(m2c1);
    }

    private static class TestComponentProcessor implements ComponentProcessor {
        final List<PowerMeasure.TimestampedValue> values = new ArrayList<>();

        @Override
        public void recordComponentValue(double value, long timestamp) {
            values.add(new PowerMeasure.TimestampedValue(timestamp, value));
        }
    }
}
