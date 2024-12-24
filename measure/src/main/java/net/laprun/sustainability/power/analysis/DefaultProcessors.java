package net.laprun.sustainability.power.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.laprun.sustainability.power.SensorMetadata;

public class DefaultProcessors implements Processors {
    private final List<ComponentProcessor>[] processors;
    private boolean hasComponentProcessors = false;
    private List<MeasureProcessor> measureProcessors;

    @SuppressWarnings("unchecked")
    public DefaultProcessors(int componentCardinality) {
        this.processors = new List[componentCardinality];
    }

    @Override
    public void recordMeasure(double[] components, long timestamp) {
        if (measureProcessors != null) {
            measureProcessors.forEach(processor -> processor.recordMeasure(components, timestamp));
        }

        for (var index = 0; index < components.length; index++) {
            recordComponentValue(components[index], timestamp, index);
        }
    }

    private void recordComponentValue(double value, long timestamp, int componentIndex) {
        final var componentProcessors = processors[componentIndex];
        if (componentProcessors != null) {
            componentProcessors.forEach(proc -> proc.recordComponentValue(value, timestamp));
        }
    }

    @Override
    public void recordSyntheticComponentValue(double syntheticValue, long timestamp, int componentIndex) {
        recordComponentValue(syntheticValue, componentIndex, componentIndex);
    }

    @Override
    public void registerProcessorFor(int componentIndex, ComponentProcessor processor) {
        if (processor != null) {
            var processorsForComponent = processors[componentIndex];
            if (processorsForComponent == null) {
                processorsForComponent = new ArrayList<>();
                processors[componentIndex] = processorsForComponent;
            }
            processorsForComponent.add(processor);
            hasComponentProcessors = true;
        }
    }

    @Override
    public List<ComponentProcessor> processorsFor(int componentIndex) {
        final var componentProcessors = processors[componentIndex];
        return Objects.requireNonNullElseGet(componentProcessors, List::of);
    }

    @Override
    public void registerMeasureProcessor(MeasureProcessor processor) {
        if (processor != null) {
            if (measureProcessors == null) {
                measureProcessors = new ArrayList<>();
            }
            measureProcessors.add(processor);
        }
    }

    @Override
    public List<MeasureProcessor> measureProcessors() {
        return Objects.requireNonNullElseGet(measureProcessors, List::of);
    }

    @Override
    public String output(SensorMetadata metadata) {
        StringBuilder builder = new StringBuilder();
        final var measureProcs = measureProcessors();
        if (!measureProcs.isEmpty()) {
            builder.append("# Measure Processors\n");
            for (var processor : measureProcs) {
                builder.append("  * ").append(processor.name()).append(": ").append(processor.output()).append("\n");
            }
        }
        if (hasComponentProcessors) {
            builder.append("# Component Processors\n");
            for (int i = 0; i < processors.length; i++) {
                final var componentProcessors = processorsFor(i);
                if (!componentProcessors.isEmpty()) {
                    final var name = metadata.metadataFor(i).name();
                    builder.append("  - ").append(name).append(" component:\n");
                    for (var processor : componentProcessors) {
                        builder.append("    * ").append(processor.name())
                                .append(": ").append(processor.output()).append("\n");
                    }
                }
            }
        }
        return builder.toString();
    }
}
