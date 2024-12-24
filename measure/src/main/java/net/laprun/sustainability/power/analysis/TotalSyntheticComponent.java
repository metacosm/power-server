package net.laprun.sustainability.power.analysis;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

public class TotalSyntheticComponent implements SyntheticComponent {
    private final Function<double[], Double> formula;
    private final SensorUnit expectedResultUnit;
    private final SensorMetadata.ComponentMetadata metadata;

    public TotalSyntheticComponent(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        Objects.requireNonNull(totalComponentIndices, "Must specify component indices that will aggregated in a total");
        this.expectedResultUnit = Objects.requireNonNull(expectedResultUnit, "Must specify expected result unit");

        final var errors = new Errors();
        final var totalComponents = Arrays.stream(totalComponentIndices)
                .mapToObj(i -> toTotalComponent(metadata, i, errors))
                .toArray(TotalComponent[]::new);
        final String description = Arrays.stream(totalComponents)
                .map(TotalComponent::name)
                .collect(Collectors.joining(" + ", "Aggregated total from (", ")"));
        final String name = Arrays.stream(totalComponents)
                .map(TotalComponent::name)
                .collect(Collectors.joining("_", "total", ""));
        final var isAttributed = metadata.components().values().stream()
                .map(SensorMetadata.ComponentMetadata::isAttributed)
                .reduce(Boolean::logicalAnd).orElse(false);
        formula = components -> {
            double result = 0;
            for (var totalComponent : totalComponents) {
                result += components[totalComponent.index] * totalComponent.factor;
            }
            return result;
        };

        if (metadata.exists(name)) {
            errors.addError("Component " + name + " already exists");
        }

        if (errors.hasErrors()) {
            throw new IllegalArgumentException(errors.formatErrors());
        }

        this.metadata = new SensorMetadata.ComponentMetadata(name, description, isAttributed, expectedResultUnit);
    }

    private double convertToExpectedUnit(double value) {
        return value * expectedResultUnit.base().conversionFactorTo(expectedResultUnit);
    }

    private record TotalComponent(String name, int index, double factor) {
    }

    private TotalComponent toTotalComponent(SensorMetadata metadata, int index, Errors errors) {
        final var cm = metadata.metadataFor(index);
        final var name = cm.name();
        final var unit = cm.unit();
        if (!unit.isCommensurableWith(expectedResultUnit)) {
            errors.addError("Component " + name
                    + " is not commensurable with the expected base unit: " + expectedResultUnit);
        }

        final var factor = unit.factor();
        return new TotalComponent(name, index, factor);
    }

    @Override
    public SensorMetadata.ComponentMetadata metadata() {
        return metadata;
    }

    @Override
    public double synthesizeFrom(double[] components, long timestamp) {
        return convertToExpectedUnit(formula.apply(components));
    }
}
