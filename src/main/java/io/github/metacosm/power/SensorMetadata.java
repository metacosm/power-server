package io.github.metacosm.power;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public interface SensorMetadata {
    record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit){};

    ComponentMetadata metadataFor(String component);

    int componentCardinality();

    @JsonProperty("metadata")
    Map<String, ComponentMetadata> getComponents();
}
