package net.laprun.sustainability.power.analysis.total;

import net.laprun.sustainability.power.Measure;
import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.SensorUnit;
import net.laprun.sustainability.power.analysis.SyntheticComponent;

public class TotalSyntheticComponent implements SyntheticComponent {
    private final Totaler totaler;
    private final SensorMetadata.ComponentMetadata metadata;

    public TotalSyntheticComponent(SensorMetadata metadata, SensorUnit expectedResultUnit, int... totalComponentIndices) {
        this(new Totaler(metadata, expectedResultUnit, totalComponentIndices));
    }

    private TotalSyntheticComponent(Totaler totaler) {
        this.totaler = totaler;
        final var name = totaler.name();
        this.metadata = new SensorMetadata.ComponentMetadata(name, "Aggregated " + name, totaler.isAttributed(),
                totaler.expectedResultUnit());
    }

    @Override
    public SensorMetadata.ComponentMetadata metadata() {
        return metadata;
    }

    @Override
    public double synthesizeFrom(double[] components, long timestamp) {
        return totaler.computeTotalFrom(components);
    }

    public Measure asMeasure(double[] components) {
        return new Measure(synthesizeFrom(components, 0L), metadata.unit());
    }
}
