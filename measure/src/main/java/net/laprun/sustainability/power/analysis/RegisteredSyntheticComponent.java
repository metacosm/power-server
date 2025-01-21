package net.laprun.sustainability.power.analysis;

import net.laprun.sustainability.power.SensorMetadata;

public class RegisteredSyntheticComponent implements SyntheticComponent {
    private final SyntheticComponent syntheticComponent;
    private final int computedIndex;

    public RegisteredSyntheticComponent(SyntheticComponent syntheticComponent, int computedIndex) {
        this.syntheticComponent = syntheticComponent;
        this.computedIndex = computedIndex;
    }

    @Override
    public SensorMetadata.ComponentMetadata metadata() {
        return syntheticComponent.metadata();
    }

    @Override
    public double synthesizeFrom(double[] components, long timestamp) {
        return syntheticComponent.synthesizeFrom(components, timestamp);
    }

    public int computedIndex() {
        return computedIndex;
    }

    public SyntheticComponent syntheticComponent() {
        return syntheticComponent;
    }
}
