package net.laprun.sustainability.power.analysis.total;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

/**
 * Totaler provides a way to compute the aggregated total for a subset of sensor measures, ensuring that the proper conversions
 * between commensurate units are used.
 */
public class Totaler {
    private final SensorUnit expectedResultUnit;
    private final Function<double[], Double> formula;
    private final String name;
    private final int[] totalComponentIndices;
    private final boolean isAttributed;
    private Errors errors;

    /**
     * Create a new Totaler instance working with the specified {@link SensorMetadata} and computing a total measure using the
     * provided expected result unit, adding (and converting to the target unit, if needed) values for the provided component
     * indices. If no indices are provided, components will be automatically chosen as follows: only components using units
     * compatible with the target unit will be used, and, among these, as mixing attributed and non-attributed values will
     * result in useless results, priority will be given to attributed components. If no attributed component is found, then
     * unattributed components will be considered.
     *
     * @param metadata the sensor metadata providing the component information to use as basis to compute an aggregate value
     * @param expectedResultUnit a {@link SensorUnit} representing the unit with which the aggregate should be calculated
     * @param totalComponentIndices optional list of component indices to take into account, if no such indices are provided,
     *        the Totaler will select "appropriate" indices automatically, if possible
     */
    public Totaler(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        this.expectedResultUnit = Objects.requireNonNull(expectedResultUnit, "Must specify expected result unit");

        errors = new Errors();

        final TotalComponent[] totalComponents;
        final var attributed = new Boolean[1];
        if (totalComponentIndices == null || totalComponentIndices.length == 0) {
            // automatically aggregate components commensurate with the expected result unit
            // first, only select attributed components
            var maybeComponents = createTotalComponents(metadata, expectedResultUnit, true);
            attributed[0] = true;
            // if there are no commensurate attributed components, look for unattributed ones
            if (maybeComponents.length == 0) {
                maybeComponents = createTotalComponents(metadata, expectedResultUnit, false);
                attributed[0] = false;
            }
            if (maybeComponents.length == 0) {
                addError("No components are compatible with the expected result unit " + expectedResultUnit);
                validate(); // exit immediately
            }

            // record total indices
            totalComponents = maybeComponents;
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

    private TotalComponent[] createTotalComponents(SensorMetadata metadata, SensorUnit expectedResultUnit,
            boolean attributed) {
        return metadata.components().values().stream()
                .filter(cm -> attributed == cm.isAttributed())
                .filter(cm -> cm.unit().isCommensurableWith(expectedResultUnit))
                .map(cm -> new TotalComponent(cm.name(), cm.index(), cm.unit().factor(), cm.isAttributed()))
                .toArray(TotalComponent[]::new);
    }

    private boolean checkAggregatedAttribution(boolean isComponentAttributed, Boolean[] aggregateAttribution) {
        var currentAttribution = aggregateAttribution[0];
        if (currentAttribution == null) {
            currentAttribution = isComponentAttributed;
        } else {
            if (currentAttribution != isComponentAttributed) {
                addError(Errors.ATTRIBUTION_MIX_ERROR);
            }
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
                    " doesn't contain components for required total indices: " + Arrays.toString(totalComponentIndices));
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
