package net.laprun.sustainability.power.analysis;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.laprun.sustainability.power.Errors;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;

public class TotalMeasureProcessor implements MeasureProcessor {
    private final String name;
    private double minTotal = Double.MAX_VALUE;
    private double maxTotal;
    private double accumulatedTotal;
    private final Function<double[], Double> formula;

    public TotalMeasureProcessor(SensorMetadata metadata, int... totalComponentIndices) {
        Objects.requireNonNull(totalComponentIndices, "Must specify component indices that will aggregated in a total");

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
        if (!cm.isWattCommensurable()) {
            errors.addError("Component " + name
                    + " is not commensurate with a power measure. It needs to be expressible in Watts.");
        }

        final var factor = SensorUnit.of(cm.unit()).getUnit().factor();
        return new TotalComponent(name, index, factor);
    }

    public double total() {
        return accumulatedTotal;
    }

    public double minMeasuredTotal() {
        return minTotal;
    }

    public double maxMeasuredTotal() {
        return maxTotal;
    }

    private record TotalComponent(String name, int index, double factor) {
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String output() {
        return MeasureProcessor.super.output();
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
