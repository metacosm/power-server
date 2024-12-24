package net.laprun.sustainability.power.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

class TotalMeasureProcessorTest {

    @Test
    void totalShouldFailIfAllComponentsAreNotCommensurable() {
        final var inError = "cp2";
        final var metadata = SensorMetadata
                .withNewComponent("cp1", null, true, "mW")
                .withNewComponent(inError, null, true, "mJ")
                .build();

        final var expectedResultUnit = SensorUnit.W;
        var e = assertThrows(IllegalArgumentException.class,
                () -> new TotalMeasureProcessor(metadata, expectedResultUnit, 0, 1));
        assertTrue(e.getMessage().contains("Component " + inError
                + " is not commensurable with the expected base unit: " + expectedResultUnit));

        e = assertThrows(IllegalArgumentException.class,
                () -> new TotalSyntheticComponent(metadata, expectedResultUnit, 0, 1));
        assertTrue(e.getMessage().contains("Component " + inError
                + " is not commensurable with the expected base unit: " + expectedResultUnit));
    }

    @Test
    void testTotal() {
        final var metadata = SensorMetadata
                .withNewComponent("cp1", null, true, "mW")
                .withNewComponent("cp2", null, true, "mW")
                .withNewComponent("cp3", null, true, "mW")
                .build();

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

        final var totalProc = new TotalMeasureProcessor(metadata, SensorUnit.of("mW"), 0, 1, 2);
        final var totalSyncComp = new TotalSyntheticComponent(metadata, SensorUnit.W, 0, 1, 2);

        final var components = new double[metadata.componentCardinality()];
        components[0] = m1c1;
        components[1] = m1c2;
        components[2] = m1c3;
        totalProc.recordMeasure(components, System.currentTimeMillis());
        // original components use mW as unit but we're asking for a synthetic total in W so the resulting total should be factored
        final var mWtoWFactor = SensorUnit.mW.conversionFactorTo(SensorUnit.W);
        assertEquals(m1total * mWtoWFactor, totalSyncComp.synthesizeFrom(components, 0));

        components[0] = m2c1;
        components[1] = m2c2;
        components[2] = m2c3;
        totalProc.recordMeasure(components, System.currentTimeMillis());
        assertEquals(m2total * mWtoWFactor, totalSyncComp.synthesizeFrom(components, 0));

        components[0] = m3c1;
        components[1] = m3c2;
        components[2] = m3c3;
        totalProc.recordMeasure(components, System.currentTimeMillis());
        assertEquals(m3total * mWtoWFactor, totalSyncComp.synthesizeFrom(components, 0));

        assertEquals(m1c1 + m1c2 + m1c3 + m2c1 + m2c2 + m2c3 + m3c1 + m3c2 + m3c3, totalProc.total());
        assertEquals(Stream.of(m1total, m2total, m3total).min(Double::compareTo).orElseThrow(), totalProc.minMeasuredTotal());
        assertEquals(Stream.of(m1total, m2total, m3total).max(Double::compareTo).orElseThrow(), totalProc.maxMeasuredTotal());

        assertTrue(totalProc.output().contains("" + totalProc.total()));
    }

}
