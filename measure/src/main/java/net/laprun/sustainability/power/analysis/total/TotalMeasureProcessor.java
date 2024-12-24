package net.laprun.sustainability.power.analysis.total;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.MeasureProcessor;

public class TotalMeasureProcessor implements MeasureProcessor {
    private final String name;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private double accumulatedTotal;
    private final Function<double[], Double> formula;
    private final SensorUnit expectedResultUnit;

    public TotalMeasureProcessor(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        Objects.requireNonNull(totalComponentIndices, "Must specify component indices that will aggregated in a total");
        this.expectedResultUnit = Objects.requireNonNull(expectedResultUnit, "Must specify expected result unit");

        final var errors = new Errors();
        final var totalComponents = Arrays.stream(totalComponentIndices)
                .mapToObj(i -> toTotalComponent(metadata, i, errors))
                .toArray(TotalComponent[]::new);
        name = Arrays.stream(totalComponents)
                .map(TotalComponent::name)
                .collect(Collectors.joining(" + ", "Aggregated total from (", ")"));
        formula = components -> {
            double result = 0;
            for (var totalComponent : totalComponents) {
                result += components[totalComponent.index] * totalComponent.factor;
            }
            return result;
        };

        if (errors.hasErrors()) {
            throw new IllegalArgumentException(errors.formatErrors());
        }
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

    public double total() {
        return convertToExpectedUnit(accumulatedTotal);
    }

    public double minMeasuredTotal() {
        return minTotal == Double.MAX_VALUE ? 0.0 : convertToExpectedUnit(minTotal);
    }

    public double maxMeasuredTotal() {
        return convertToExpectedUnit(maxTotal);
    }

    private double convertToExpectedUnit(double value) {
        return value * expectedResultUnit.base().conversionFactorTo(expectedResultUnit);
    }

    private record TotalComponent(String name, int index, double factor) {
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String output() {
        final var symbol = expectedResultUnit.symbol();
        return String.format("%.2f%s (min: %.2f / max: %.2f)", total(), symbol, minMeasuredTotal(), maxMeasuredTotal());
    }

    @Override
    public void recordMeasure(double[] measure, long timestamp) {
        final double recordedTotal = formula.apply(measure);
        accumulatedTotal += recordedTotal;
        if (recordedTotal < minTotal) {
            minTotal = recordedTotal;
        }
        if (recordedTotal > maxTotal) {
            maxTotal = recordedTotal;
        }
    }
}
