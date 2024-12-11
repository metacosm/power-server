package net.laprun.sustainability.power.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.laprun.sustainability.power.SensorMetadata;

public class DefaultProcessors implements Processors {
    private final List<ComponentProcessor>[] processors;
    private ComponentProcessor totalProcessor;

    @SuppressWarnings("unchecked")
    public DefaultProcessors(int componentCardinality) {
        this.processors = new List[componentCardinality];
    }

    @Override
    public void recordMeasure(double[] components, long timestamp) {
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
    public void registerTotalProcessor(ComponentProcessor processor) {
        this.totalProcessor = processor;
    }

    @Override
    public void recordTotal(double total, long timestamp) {
        if (totalProcessor != null) {
            totalProcessor.recordComponentValue(total, timestamp);
        }
    }

    @Override
    public Optional<ComponentProcessor> totalProcessor() {
        return Optional.ofNullable(totalProcessor);
    }

    @Override
    public List<ComponentProcessor> processorsFor(int componentIndex) {
        final var componentProcessors = processors[componentIndex];
        return Objects.requireNonNullElseGet(componentProcessors, List::of);
    }

    @Override
    public <T extends ComponentProcessor> Optional<T> processorFor(int componentIndex, Class<T> expectedType) {
        final var componentProcessors = processors[componentIndex];
        if (componentProcessors != null) {
            return componentProcessors.stream().filter(expectedType::isInstance).map(expectedType::cast).findFirst();
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String output(SensorMetadata metadata) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Processors\n");
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
