package net.laprun.sustainability.power.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.measure.OngoingPowerMeasure;

public class ComputeTest {
    private final static SensorMetadata metadata = SensorMetadata
            .withNewComponent("cp1", null, true, SensorUnit.mW)
            .withNewComponent("cp2", null, true, SensorUnit.mW)
            .withNewComponent("cp3", null, true, "mW")
            .build();

    @Test
    void standardDeviationShouldWork() {
        final var random = Random.from(RandomGenerator.getDefault());
        final var m1c1 = random.nextDouble();
        final var m1c2 = random.nextDouble();
        final var m1c3 = 0.0;
        final var m2c1 = random.nextDouble();
        final var m2c2 = random.nextDouble();
        final var m2c3 = 0.0;
        final var m3c1 = random.nextDouble();
        final var m3c2 = random.nextDouble();
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

        final var c1Avg = Compute.average(measure, 0);
        final var c2Avg = Compute.average(measure, 1);
        final var stdVarForC1 = Math
                .sqrt((Math.pow(m1c1 - c1Avg, 2) + Math.pow(m2c1 - c1Avg, 2) + Math.pow(m3c1 - c1Avg, 2)) / (3 - 1));
        final var stdVarForC2 = Math
                .sqrt((Math.pow(m1c2 - c2Avg, 2) + Math.pow(m2c2 - c2Avg, 2) + Math.pow(m3c2 - c2Avg, 2)) / (3 - 1));

        assertEquals(stdVarForC1, Compute.standardDeviation(measure, 0), 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(stdVarForC2, Compute.standardDeviation(measure, 1), 0.0001,
                "Standard Deviation did not match the expected value");
        assertEquals(0, Compute.standardDeviation(measure, 2), 0.0001,
                "Standard Deviation did not match the expected value");
    }

    @Test
    void averageShouldWork() {
        final var random = Random.from(RandomGenerator.getDefault());
        final var m1c1 = random.nextDouble();
        final var m1c2 = random.nextDouble();
        final var m1c3 = 0.0;
        final var m2c1 = random.nextDouble();
        final var m2c2 = random.nextDouble();
        final var m2c3 = 0.0;
        final var m3c1 = random.nextDouble();
        final var m3c2 = random.nextDouble();
        final var m3c3 = 0.0;

        final var measure = new OngoingPowerMeasure(metadata);
        final var avgProc1 = new MeanComponentProcessor();
        measure.registerProcessorFor(0, avgProc1);
        final var avgProc2 = new MeanComponentProcessor();
        measure.registerProcessorFor(1, avgProc2);
        final var avgProc3 = new MeanComponentProcessor();
        measure.registerProcessorFor(2, avgProc3);

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

        final var c1Avg = Compute.average(measure, 0);
        final var c2Avg = Compute.average(measure, 1);
        final var c3Avg = Compute.average(measure, 2);

        assertEquals((m1c1 + m2c1 + m3c1) / 3, c1Avg, 0.0001,
                "Average did not match the expected value");
        assertEquals(c1Avg, avgProc1.mean(), 0.0001);
        assertEquals((m1c2 + m2c2 + m3c2) / 3, c2Avg, 0.0001,
                "Average did not match the expected value");
        assertEquals(c2Avg, avgProc2.mean(), 0.0001);
        assertEquals(0, c3Avg, 0.0001,
                "Average did not match the expected value");
        assertEquals(0, avgProc3.mean());
    }
}
