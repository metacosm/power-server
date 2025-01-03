package net.laprun.sustainability.power.analysis.total;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.SyntheticComponent;

public class TotalSyntheticComponent implements SyntheticComponent {
    private final Totaler totaler;
    private final SensorMetadata.ComponentMetadata metadata;

    public TotalSyntheticComponent(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        this.totaler = new Totaler(metadata, expectedResultUnit, totalComponentIndices);
        final var isAttributed = metadata.components().values().stream()
                .map(SensorMetadata.ComponentMetadata::isAttributed)
                .reduce(Boolean::logicalAnd).orElse(false);
        final var name = totaler.name();
        if (metadata.exists(name)) {
            totaler.addError("Component " + name + " already exists");
        }

        totaler.validate();

        this.metadata = new SensorMetadata.ComponentMetadata(name, "Aggregated " + name, isAttributed, expectedResultUnit);
    }

    @Override
    public SensorMetadata.ComponentMetadata metadata() {
        return metadata;
    }

    @Override
    public double synthesizeFrom(double[] components, long timestamp) {
        return totaler.computeTotalFrom(components);
    }
}
