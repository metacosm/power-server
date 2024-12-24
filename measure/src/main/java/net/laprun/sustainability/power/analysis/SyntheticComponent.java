package net.laprun.sustainability.power.analysis;

import net.laprun.sustainability.power.SensorMetadata;

public interface SyntheticComponent {

    SensorMetadata.ComponentMetadata metadata();

    double synthesizeFrom(double[] components, long timestamp);
}
