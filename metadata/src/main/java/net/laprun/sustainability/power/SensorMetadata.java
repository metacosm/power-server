package net.laprun.sustainability.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The metadata associated with a power-consumption recording sensor. This allows to make sense of the data sent by the power
 * server by providing information about each component (e.g. CPU) recorded by the sensor during each periodical measure.
 */
public class SensorMetadata {
    @JsonProperty("metadata")
    private final Map<String, ComponentMetadata> components;
    @JsonProperty("documentation")
    private final String documentation;
    @JsonProperty("totalComponents")
    private final int[] totalComponents;

    /**
     * Initializes sensor metadata information
     *
     * @param components a map describing the metadata for each component
     * @param documentation a text providing any relevant information associated with the described sensor
     * @throws IllegalArgumentException if indices specified in {@code totalComponents} do not represent power measures
     *         expressible in Watts or are not a valid index
     */
    public SensorMetadata(List<ComponentMetadata> components, String documentation) {
        Objects.requireNonNull(components, "Must provide components");
        final var cardinality = components.size();
        this.components = new HashMap<>(cardinality);
        this.documentation = documentation;
        final var errors = new Errors();
        final var indices = new BitSet(cardinality);
        final var totalIndices = new BitSet(cardinality);
        components.forEach(component -> {
            // check that index is valid
            final var index = component.index;
            boolean indexValid = true;
            if (index < 0 || index >= cardinality) {
                errors.addError(index + " is not a valid index: must be between 0 and " + (cardinality - 1));
                indexValid = false;
            } else if (indices.get(index)) {
                errors.addError("Multiple components are using index " + index + ": "
                        + components.stream().filter(cm -> index == cm.index).toList());
                indexValid = false;
            } else {
                // record index as known
                indices.set(index);
            }

            // check that component's unit is commensurable to Watts if included in total
            if (component.isIncludedInTotal) {
                if (indexValid) {
                    totalIndices.set(index);
                }

                if (!component.isWattCommensurable()) {
                    errors.addError("Component " + component.name
                            + " is not commensurate with a power measure. It needs to be expressible in Watts.");
                }
            }

            if (this.components.containsKey(component.name)) {
                errors.addError("Multiple components are named '" + component.name + "': "
                        + components.stream().filter(cm -> cm.name.equals(component.name)).toList());
            } else {
                this.components.put(component.name, component);
            }
        });

        // verify that all indices are covered
        if (indices.cardinality() != cardinality) {
            indices.flip(0, cardinality);
            errors.addError(
                    "Components' indices should cover the full range of 0 to " + (cardinality - 1) + ". Missing indices: "
                            + indices);
        }

        if (errors.hasErrors()) {
            throw new IllegalArgumentException(errors.formatErrors());
        }

        this.totalComponents = totalIndices.stream().toArray();
    }

    @JsonCreator
    SensorMetadata(Map<String, ComponentMetadata> components, String documentation, int[] totalComponents) {
        this.components = components;
        this.documentation = documentation;
        this.totalComponents = totalComponents;
    }

    public static SensorMetadata.Builder withNewComponent(String name, String description, boolean isAttributed, String unit,
            boolean participatesInTotal) {
        return new SensorMetadata.Builder().withNewComponent(name, description, isAttributed, unit, participatesInTotal);
    }

    public static SensorMetadata.Builder from(SensorMetadata sensorMetadata) {
        final var builder = new Builder();
        sensorMetadata.components.values().stream().sorted(Comparator.comparing(ComponentMetadata::index))
                .forEach(component -> builder.withNewComponent(component.name, component.description, component.isAttributed,
                        component.unit,
                        component.isIncludedInTotal));
        return builder;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        components.values().stream().sorted(Comparator.comparing(ComponentMetadata::index))
                .forEach(cm -> sb.append("- ").append(cm).append("\n"));
        return "components:\n"
                + sb
                + "documentation: " + documentation + "\n"
                + "totalComponents: " + Arrays.toString(totalComponents);
    }

    /**
     * Determines whether a component with the specified name is known for this sensor
     *
     * @param component the name of the component
     * @return {@code true} if a component with the specified name exists for the associated sensor, {@code false} otherwise
     */
    public boolean exists(String component) {
        return components.containsKey(component);
    }

    /**
     * Retrieves the {@link ComponentMetadata} associated with the specified component name if it exists
     *
     * @param component the name of the component which metadata is to be retrieved
     * @return the {@link ComponentMetadata} associated with the specified name
     * @throws IllegalArgumentException if no component with the specified name is known for the associated sensor
     */
    public ComponentMetadata metadataFor(String component) throws IllegalArgumentException {
        final var componentMetadata = components.get(component);
        if (componentMetadata == null) {
            throw new IllegalArgumentException("Unknown component: " + component);
        }
        return componentMetadata;
    }

    /**
     * Retrieves the number of known components for the associated sensor
     *
     * @return the cardinality of known components
     */
    public int componentCardinality() {
        return components.size();
    }

    /**
     * Retrieves the known {@link ComponentMetadata} for the associated sensor as an unmodifiable Map keyed by the components'
     * name
     *
     * @return an unmodifiable Map of the known {@link ComponentMetadata}
     */
    public Map<String, ComponentMetadata> components() {
        return Collections.unmodifiableMap(components);
    }

    /**
     * Retrieves the documentation, if any, associated with this SensorMetadata
     *
     * @return the documentation relevant for the associated sensor
     */
    public String documentation() {
        return documentation;
    }

    /**
     * Retrieves the indices of the components that can be used to compute a total
     *
     * @return the indices of the components that can be used to compute a total
     */
    public int[] totalComponents() {
        return totalComponents;
    }

    /**
     * Retrieves the metadata associated with the specified component index if it exists.
     *
     * @param componentIndex the index of the component we want to retrieve the metadata for
     * @return the {@link ComponentMetadata} associated with the specified index if it exists
     * @throws IllegalArgumentException if no component is associated with the specified index
     */
    public ComponentMetadata metadataFor(int componentIndex) {
        return components.values().stream()
                .filter(cm -> componentIndex == cm.index)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No component was found for index " + componentIndex));
    }

    @SuppressWarnings("unused")
    public static class Builder {
        private final List<ComponentMetadata> components = new ArrayList<>();
        private int currentIndex = 0;
        private String documentation;

        public Builder withNewComponent(String name, String description, boolean isAttributed, String unit,
                boolean isIncludedInTotal) {
            components.add(new ComponentMetadata(name, currentIndex++, description, isAttributed, unit, isIncludedInTotal));
            return this;
        }

        public Builder withDocumentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public SensorMetadata build() {
            return new SensorMetadata(components, documentation);
        }
    }

    /**
     * The information associated with a recorded component
     *
     * @param name the name of the component (e.g. CPU)
     * @param index the index at which the measure for this component is recorded in the {@link SensorMeasure#components()}
     *        array
     * @param description a short textual description of what this component is about when available (for automatically
     *        extracted components, this might be identical to the name)
     * @param isAttributed whether or not this component provides an attributed value i.e. whether the value is already computed
     *        for the process during a measure or, on the contrary, if the measure is done globally and the computation of the
     *        attributed share for each process needs to be performed. This is needed because some sensors only provide
     *        system-wide measures instead of on a per-process basis.
     * @param unit a textual representation of the unit used for measures associated with this component (e.g. mW)
     * @param isIncludedInTotal whether or not this component takes part in the computation to get a total power consumption
     *        metric for that sensor. Components that take part of the total computation must use a unit commensurable with
     *        {@link SensorUnit#W}
     */
    public record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit,
            boolean isIncludedInTotal) {

        public ComponentMetadata {
            if (name == null) {
                throw new IllegalArgumentException("Component name cannot be null");
            }
        }

        /**
         * Determines whether or not this component is measuring power (i.e. its value can be converted to Watts)
         *
         * @return {@code true} if this component's unit is commensurable to Watts, {@code false} otherwise
         */
        @JsonIgnore
        public boolean isWattCommensurable() {
            return unit != null && SensorUnit.of(unit).isWattCommensurable();
        }
    }
}
