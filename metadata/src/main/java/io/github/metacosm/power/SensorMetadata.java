package io.github.metacosm.power;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SensorMetadata {
    public record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit){};

    public SensorMetadata(Map<String, ComponentMetadata> components) {
        this.components = components;
    }

    @JsonProperty("metadata")
    private final Map<String, ComponentMetadata> components;

    public ComponentMetadata metadataFor(String component) {
        final var componentMetadata = components.get(component);
        if (componentMetadata == null) {
            throw new IllegalArgumentException("Unknown component: " + component);
        }
        return componentMetadata;
    }

    public int componentCardinality() {
        return components.size();
    }
}
