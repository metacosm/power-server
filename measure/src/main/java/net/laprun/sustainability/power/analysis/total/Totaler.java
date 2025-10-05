package net.laprun.sustainability.power.analysis.total;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

class Totaler {
    private final SensorUnit expectedResultUnit;
    private final Function<double[], Double> formula;
    private final String name;
    private final int[] totalComponentIndices;
    private Errors errors;

    Totaler(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        Objects.requireNonNull(totalComponentIndices, "Must specify component indices that will aggregated in a total");
        this.expectedResultUnit = Objects.requireNonNull(expectedResultUnit, "Must specify expected result unit");

        errors = new Errors();
        final var totalComponents = Arrays.stream(totalComponentIndices)
                .mapToObj(i -> from(metadata, i, expectedResultUnit, errors))
                .toArray(TotalComponent[]::new);
        name = Arrays.stream(totalComponents)
                .map(TotalComponent::name)
                .collect(Collectors.joining(" + ", "total (", ")"));
        formula = formulaFrom(totalComponents);
        this.totalComponentIndices = totalComponentIndices;
    }

    void validate() {
        if (errors.hasErrors()) {
            throw new IllegalArgumentException(errors.formatErrors());
        }
        errors = null;
    }

    public void addError(String message) {
        if (errors == null) {
            throw new IllegalStateException("Totaler has already been validated!");
        }
        errors.addError(message);
    }

    public String name() {
        return name;
    }

    public SensorUnit expectedResultUnit() {
        return expectedResultUnit;
    }

    public double computeTotalFrom(double[] measure) {
        if (measure.length < totalComponentIndices.length) {
            throw new IllegalArgumentException("Provided measure " + Arrays.toString(measure) +
                    " doesn't countain components for required total indices: " + Arrays.toString(totalComponentIndices));
        }

        checkValidated();
        return convertToExpectedUnit(formula.apply(measure));
    }

    private void checkValidated() {
        if (errors != null) {
            throw new IllegalStateException("Totaler must be validated before use!");
        }
    }

    private double convertToExpectedUnit(double value) {
        return value * expectedResultUnit.base().conversionFactorTo(expectedResultUnit);
    }

    private record TotalComponent(String name, int index, double factor) {
        double scaledValueFrom(double[] componentValues) {
            return componentValues[index] * factor;
        }
    }

    private static TotalComponent from(SensorMetadata metadata, int index, SensorUnit expectedResultUnit, Errors errors) {
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

    private static Function<double[], Double> formulaFrom(TotalComponent[] totalComponents) {
        return components -> {
            double result = 0;
            for (var totalComponent : totalComponents) {
                result += totalComponent.scaledValueFrom(components);
            }
            return result;
        };
    }
}
