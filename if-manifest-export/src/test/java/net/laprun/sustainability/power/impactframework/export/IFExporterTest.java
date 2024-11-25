package net.laprun.sustainability.power.impactframework.export;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.measure.OngoingPowerMeasure;
import net.laprun.sustainability.power.measure.StoppedPowerMeasure;

class IFExporterTest {
    public static final String COMPONENT1_NAME = "c1";
    public static final String COMPONENT2_NAME = "c2";
    public static final String COMPONENT3_NAME = "c3";
    private final static SensorMetadata metadata = new SensorMetadata(Map.of(
            COMPONENT1_NAME, new SensorMetadata.ComponentMetadata(COMPONENT1_NAME, 0, "component 1", true, "mW"),
            COMPONENT2_NAME, new SensorMetadata.ComponentMetadata(COMPONENT2_NAME, 1, "component 2", true, "mW"),
            COMPONENT3_NAME, new SensorMetadata.ComponentMetadata(COMPONENT3_NAME, 2, "always zero", false, "mW")), null,
            new int[] { 0, 1, 2 });

    @Test
    void export() {
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

        final var manifest = IFExporter.export(new StoppedPowerMeasure(measure));
        assertNotNull(manifest);
        final var child = manifest.tree().children().get(IFExporter.CHILD_NAME);
        assertNotNull(child);
        final var inputs = child.inputs();
        assertNotNull(inputs);
        assertEquals(3, inputs.size());
        final var input = inputs.get(1);
        final var values = input.values();
        assertEquals(m2c1, values.get(IFExporter.getInputValueName(COMPONENT1_NAME)));
        assertEquals(m2c2, values.get(IFExporter.getInputValueName(COMPONENT2_NAME)));
        assertEquals(m2c3, values.get(IFExporter.getInputValueName(COMPONENT3_NAME)));
    }
}
