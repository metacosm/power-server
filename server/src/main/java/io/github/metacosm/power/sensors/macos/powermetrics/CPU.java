package io.github.metacosm.power.sensors.macos.powermetrics;

import java.util.HashMap;
import java.util.Map;

import io.github.metacosm.power.SensorMetadata;

abstract class CPU {
    private SensorMetadata metadata;

    void addComponentIfFound(String line, Map<String, SensorMetadata.ComponentMetadata> components) {
        throw new IllegalStateException("Shouldn't be called as this processing is unneeded for this implementation");
    }

    abstract boolean doneExtractingPowerComponents(String line, HashMap<String, Number> powerComponents);

    SensorMetadata metadata() {
        return metadata;
    }

    void setMetadata(SensorMetadata metadata) {
        this.metadata = metadata;
    }

    abstract boolean doneAfterComponentsInitialization(Map<String, SensorMetadata.ComponentMetadata> components);
}
