package net.laprun.sustainability.power.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.laprun.sustainability.power.SensorMetadata;

public class DefaultProcessors implements Processors {
    private final List<ComponentProcessor>[] processors;
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
            final var fIndex = index;
            final var componentProcessors = processors[index];
            if (componentProcessors != null) {
                componentProcessors.forEach(proc -> proc.recordComponentValue(components[fIndex], timestamp));
            }
        }
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
        builder.append("# Component Processors\n");
        for (int i = 0; i < processors.length; i++) {
            final var name = metadata.metadataFor(i).name();
            builder.append("  - ").append(name).append(" component:\n");
            for (var processor : processorsFor(i)) {
                builder.append("    * ").append(processor.name())
                        .append(": ").append(processor.output()).append("\n");
            }
        }
        return builder.toString();
    }
}
