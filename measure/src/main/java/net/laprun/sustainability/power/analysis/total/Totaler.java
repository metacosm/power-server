package net.laprun.sustainability.power.analysis.total;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

public class Totaler {
    private final SensorUnit expectedResultUnit;
    private final Function<double[], Double> formula;
    private final String name;
    private final int[] totalComponentIndices;
    private final boolean isAttributed;
    private Errors errors;

    public Totaler(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        this.expectedResultUnit = Objects.requireNonNull(expectedResultUnit, "Must specify expected result unit");

        errors = new Errors();

        final TotalComponent[] totalComponents;
        final var attributed = new Boolean[1];
        if (totalComponentIndices == null || totalComponentIndices.length == 0) {
            // automatically aggregate components commensurate with the expected result unit
            totalComponents = metadata.components().values().stream()
                    .filter(cm -> cm.unit().isCommensurableWith(expectedResultUnit))
                    .map(cm -> new TotalComponent(cm.name(), cm.index(), cm.unit().factor(),
                            checkAggregatedAttribution(cm.isAttributed(), attributed)))
                    .toArray(TotalComponent[]::new);
            // record total indices
            totalComponentIndices = new int[totalComponents.length];
            int i = 0;
            for (var component : totalComponents) {
                totalComponentIndices[i++] = component.index();
            }
        } else {
            totalComponents = Arrays.stream(totalComponentIndices)
                    .mapToObj(i -> {
                        final var cm = metadata.metadataFor(i);
                        checkAggregatedAttribution(cm.isAttributed(), attributed);
                        return from(cm, expectedResultUnit, errors);
                    })
                    .toArray(TotalComponent[]::new);
        }

        name = Arrays.stream(totalComponents)
                .map(TotalComponent::name)
                .collect(Collectors.joining(" + ", "total (", ")"));
        if (metadata.exists(name)) {
            addError("Component " + name + " already exists");
        }

        isAttributed = attributed[0];

        formula = formulaFrom(totalComponents);
        this.totalComponentIndices = totalComponentIndices;
        validate();
    }

    private static boolean checkAggregatedAttribution(boolean isComponentAttributed, Boolean[] aggregateAttribution) {
        var currentAttribution = aggregateAttribution[0];
        if (currentAttribution == null) {
            currentAttribution = isComponentAttributed;
        } else {
            currentAttribution = currentAttribution && isComponentAttributed;
        }
        aggregateAttribution[0] = currentAttribution;
        return isComponentAttributed;
    }

    private void validate() {
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

    public boolean isAttributed() {
        return isAttributed;
    }

    int[] componentIndices() {
        return totalComponentIndices;
    }

    private void checkValidated() {
        if (errors != null) {
            throw new IllegalStateException("Totaler must be validated before use!");
        }
    }

    private double convertToExpectedUnit(double value) {
        return value * expectedResultUnit.base().conversionFactorTo(expectedResultUnit);
    }

    private record TotalComponent(String name, int index, double factor, boolean isAttributed) {
        double scaledValueFrom(double[] componentValues) {
            return componentValues[index] * factor;
        }
    }

    private static TotalComponent from(SensorMetadata.ComponentMetadata cm, SensorUnit expectedResultUnit, Errors errors) {
        final var name = cm.name();
        final var unit = cm.unit();
        if (!unit.isCommensurableWith(expectedResultUnit)) {
            errors.addError("Component " + name
                    + " is not commensurable with the expected base unit: " + expectedResultUnit);
        }

        final var factor = unit.factor();
        return new TotalComponent(name, cm.index(), factor, cm.isAttributed());
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
