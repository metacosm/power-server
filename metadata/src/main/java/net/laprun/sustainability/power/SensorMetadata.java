package net.laprun.sustainability.power;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * @param totalComponents an array of indices indicating which components can be used to compute a total power consumption
     *        metric for that sensor. Must use a unit commensurable with {@link SensorUnit#W}
     * @throws IllegalArgumentException if indices specified in {@code totalComponents} do not represent power measures
     *         expressible in Watts or are not a valid index
     */
    @JsonCreator
    public SensorMetadata(@JsonProperty("metadata") Map<String, ComponentMetadata> components,
            @JsonProperty("documentation") String documentation,
            @JsonProperty("totalComponents") int[] totalComponents) {
        this.components = Objects.requireNonNull(components, "Must provide components");
        this.documentation = documentation;
        this.totalComponents = Objects.requireNonNull(totalComponents, "Must provide total components");
        final var errors = new Errors();
        for (int index : totalComponents) {
            if (index < 0) {
                errors.addError(index + " is not a valid index");
                continue;
            }
            components.values().stream()
                    .filter(cm -> index == cm.index)
                    .findFirst()
                    .ifPresentOrElse(component -> {
                        if (!component.isWattCommensurable()) {
                            errors.addError("Component " + component.name
                                    + " is not commensurate with a power measure. It needs to be expressible in Watts.");
                        }
                    }, () -> errors.addError(index + " is not a valid index"));
        }
        if (errors.hasErrors()) {
            throw new IllegalArgumentException(errors.formatErrors());
        }
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

    private static class Errors {
        private List<String> errors;

        void addError(String error) {
            if (errors == null) {
                errors = new ArrayList<>();
            }
            errors.add(error);
        }

        boolean hasErrors() {
            return errors != null && !errors.isEmpty();
        }

        String formatErrors() {
            if (errors == null) {
                return "";
            }
            return errors.stream().collect(Collectors.joining("\n- ", "\n- ", ""));
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
     */
    public record ComponentMetadata(String name, int index, String description, boolean isAttributed, String unit) {
        /**
         * Determines whether or not this component is measuring power (i.e. its value can be converted to Watts)
         *
         * @return {@code true} if this component's unit is commensurable to Watts, {@code false} otherwise
         */
        @JsonIgnore
        public boolean isWattCommensurable() {
            return SensorUnit.of(unit).isWattCommensurable();
        }
    }
}
