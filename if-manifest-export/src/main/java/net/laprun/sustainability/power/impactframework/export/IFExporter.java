package net.laprun.sustainability.power.impactframework.export;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.laprun.sustainability.impactframework.manifest.Child;
import net.laprun.sustainability.impactframework.manifest.Initialize;
import net.laprun.sustainability.impactframework.manifest.Input;
import net.laprun.sustainability.impactframework.manifest.Manifest;
import net.laprun.sustainability.impactframework.manifest.Metadata;
import net.laprun.sustainability.impactframework.manifest.Plugin;
import net.laprun.sustainability.impactframework.manifest.Tree;
import net.laprun.sustainability.power.measure.PowerMeasure;
import net.laprun.sustainability.power.measure.StoppedPowerMeasure;

public enum IFExporter {
    ;

    private final static String prefix = "power-server-";
    private final static Initialize defaultInitialize = new Initialize(Map.of("sum", new Plugin("sum", "builtin", "Sum")));
    public static final String CHILD_NAME = prefix + "child";

    public static Manifest export(StoppedPowerMeasure measure) {
        final var ifMetadata = new Metadata("power-server measure", null, Set.of("power-server"));

        final var samples = measure.numberOfSamples();
        final List<Input> inputs = new ArrayList<>(samples);
        final var sensorMetadata = measure.metadata();
        final int samplingFrequency = (int) measure.duration().dividedBy(samples).toMillis();
        final var components = sensorMetadata.components();
        final String[] inputNames = new String[components.size()];
        components.values().forEach(component -> inputNames[component.index()] = getInputValueName(component.name()));

        for (int i = 0; i < samples; i++) {
            final var values = measure.getNthTimestampedMeasures(i);
            inputs.add(toInput(samplingFrequency, values, inputNames));
        }

        return new Manifest(ifMetadata, defaultInitialize,
                new Tree(Map.of(CHILD_NAME, new Child(inputs))));
    }

    public static String getInputValueName(String componentName) {
        return prefix + componentName;
    }

    private static Input toInput(int samplingFrequency, PowerMeasure.TimestampedMeasures values,
            String[] inputNames) {
        final var inputValues = new LinkedHashMap<String, Object>(inputNames.length);
        final var measures = values.measures();
        for (int i = 0; i < inputNames.length; i++) {
            inputValues.put(inputNames[i], measures[i]);
        }
        return new Input(Instant.ofEpochMilli(values.timestamp()), samplingFrequency, inputValues);
    }
}
