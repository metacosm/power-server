package net.laprun.sustainability.power.analysis;

import java.util.List;
import java.util.Optional;

import net.laprun.sustainability.power.SensorMetadata;

public interface Processors {
    Processors empty = new Processors() {
    };

    default void recordMeasure(double[] components, long timestamp) {
    }

    default void recordTotal(double total, long timestamp) {
    }

    default void registerProcessorFor(int componentIndex, ComponentProcessor processor) {
    }

    default void registerTotalProcessor(ComponentProcessor processor) {
    }

    default Optional<ComponentProcessor> totalProcessor() {
        return Optional.empty();
    }

    default List<ComponentProcessor> processorsFor(int componentIndex) {
        return List.of();
    }

    default <T extends ComponentProcessor> Optional<T> processorFor(int componentIndex, Class<T> expectedType) {
        return Optional.empty();
    }

    default String output(SensorMetadata metadata) {
        return "";
    }
}
