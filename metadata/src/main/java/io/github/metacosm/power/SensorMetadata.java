package io.github.metacosm.power;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class SensorMetadata {
    public record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit){}

    @JsonCreator
    public SensorMetadata(@JsonProperty("metadata") Map<String, ComponentMetadata> components, @JsonProperty("documentation") String documentation) {
        this.components = components;
        this.documentation = documentation;
    }

    @JsonProperty("metadata")
    private final Map<String, ComponentMetadata> components;

    @JsonProperty("documentation")
    private final String documentation;

    public boolean exists(String component) {
        return components.containsKey(component);
    }

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

    public Map<String, ComponentMetadata> components() {
        return Collections.unmodifiableMap(components);
    }

    public String documentation() {
        return documentation;
    }
}
