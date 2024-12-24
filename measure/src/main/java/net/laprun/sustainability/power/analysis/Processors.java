package net.laprun.sustainability.power.analysis;

import java.util.List;

import net.laprun.sustainability.power.SensorMetadata;

@SuppressWarnings("unused")
public interface Processors {
    Processors empty = new Processors() {
    };

    default void recordMeasure(double[] components, long timestamp) {
    }

    default void registerProcessorFor(int componentIndex, ComponentProcessor processor) {
    }

    default List<ComponentProcessor> processorsFor(int componentIndex) {
        return List.of();
    }

    default void registerMeasureProcessor(MeasureProcessor processor) {
    }

    default List<MeasureProcessor> measureProcessors() {
        return List.of();
    }

    default String output(SensorMetadata metadata) {
        return "";
    }

    default void recordSyntheticComponentValue(double syntheticValue, long timestamp, int componentIndex) {
    }
}
