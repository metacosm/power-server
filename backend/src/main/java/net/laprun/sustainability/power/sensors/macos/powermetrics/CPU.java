package net.laprun.sustainability.power.sensors.macos.powermetrics;

import java.util.HashMap;
import java.util.List;

import net.laprun.sustainability.power.SensorMetadata;

abstract class CPU {
    private SensorMetadata metadata;

    void addComponentIfFound(String line, List<SensorMetadata.ComponentMetadata> components) {
        throw new IllegalStateException("Shouldn't be called as this processing is unneeded for this implementation");
    }

    abstract boolean doneExtractingPowerComponents(String line, HashMap<String, Number> powerComponents);

    SensorMetadata metadata() {
        return metadata;
    }

    void setMetadata(SensorMetadata metadata) {
        this.metadata = metadata;
    }

    abstract boolean doneAfterComponentsInitialization(List<SensorMetadata.ComponentMetadata> components);
}
