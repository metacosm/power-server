package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMetadata;

import java.util.HashMap;
import java.util.Map;

abstract class CPU {
    private SensorMetadata metadata;

    abstract void addComponentIfFound(String line, Map<String, SensorMetadata.ComponentMetadata> components);

    abstract void extractPowerComponents(String line, HashMap<String, Integer> powerComponents);

    SensorMetadata metadata() {
        return metadata;
    }

    void setMetadata(SensorMetadata metadata) {
        this.metadata = metadata;
    }

    void initComponents(Map<String, SensorMetadata.ComponentMetadata> components) {
    }
}
