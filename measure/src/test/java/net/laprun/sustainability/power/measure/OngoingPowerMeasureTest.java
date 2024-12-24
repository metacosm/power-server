package net.laprun.sustainability.power.measure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.ComponentProcessor;
import net.laprun.sustainability.power.analysis.MeasureProcessor;
import net.laprun.sustainability.power.analysis.SyntheticComponent;

public class OngoingPowerMeasureTest {
    private final static SensorMetadata metadata = SensorMetadata
            .withNewComponent("cp1", null, true, "mW")
            .withNewComponent("cp2", null, true, "mW")
            .withNewComponent("cp3", null, true, "mW")
            .build();

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
    void processorsShouldBeCalled() {
        final var random = Random.from(RandomGenerator.getDefault());
        final var m1c1 = random.nextDouble();
        final var m1c2 = random.nextDouble();
        final var m2c1 = random.nextDouble();
        final var m2c2 = random.nextDouble();

        final var measure = new OngoingPowerMeasure(metadata);
        final var testProc = new TestComponentProcessor();
        final var measureProc = new TestMeasureProcessor();
        measure.registerProcessorFor(0, testProc);
        measure.registerMeasureProcessor(measureProc);

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
        assertThat(processors.measureProcessors()).hasSize(1);

        assertThat(testProc.values.getFirst().value()).isEqualTo(m1c1);
        assertThat(testProc.values.getLast().value()).isEqualTo(m2c1);
        assertThat(measureProc.values.getFirst().measures()).isEqualTo(new double[] { m1c1, m1c2, 0 });
        assertThat(measureProc.values.getLast().measures()).isEqualTo(new double[] { m2c1, m2c2, 0 });
    }

    @Test
    void syntheticComponentsShouldWork() {
        final var random = Random.from(RandomGenerator.getDefault());
        final var m1c1 = random.nextDouble();
        final var m2c1 = random.nextDouble();

        final var doublerName = "doubler";
        final var doubler = new SyntheticComponent() {
            @Override
            public SensorMetadata.ComponentMetadata metadata() {
                return new SensorMetadata.ComponentMetadata(doublerName, "doubler desc", true, SensorUnit.mW);
            }

            @Override
            public double synthesizeFrom(double[] components, long timestamp) {
                return components[0] * 2;
            }
        };

        final var measure = new OngoingPowerMeasure(metadata, doubler);
        final var testProc = new TestComponentProcessor();
        // need to get updated metadata
        final var doublerIndex = measure.metadata().metadataFor(doublerName).index();
        measure.registerProcessorFor(doublerIndex, testProc);

        final var components = new double[metadata.componentCardinality()];
        components[0] = m1c1;
        measure.recordMeasure(components);

        components[0] = m2c1;
        measure.recordMeasure(components);

        assertThat(testProc.values.getFirst().value()).isEqualTo(m1c1 * 2);
        assertThat(testProc.values.getLast().value()).isEqualTo(m2c1 * 2);
    }

    private static class TestComponentProcessor implements ComponentProcessor {
        final List<PowerMeasure.TimestampedValue> values = new ArrayList<>();

        @Override
        public void recordComponentValue(double value, long timestamp) {
            values.add(new PowerMeasure.TimestampedValue(timestamp, value));
        }
    }

    private static class TestMeasureProcessor implements MeasureProcessor {
        final List<PowerMeasure.TimestampedMeasures> values = new ArrayList<>();

        @Override
        public void recordMeasure(double[] measure, long timestamp) {
            final var copy = new double[measure.length];
            System.arraycopy(measure, 0, copy, 0, measure.length);
            values.add(new PowerMeasure.TimestampedMeasures(timestamp, copy));
        }
    }
}
