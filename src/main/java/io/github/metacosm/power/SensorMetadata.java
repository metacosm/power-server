package io.github.metacosm.power;

public interface SensorMetadata {
    record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit){};

    ComponentMetadata metadataFor(String component);

    int componentCardinality();
}
