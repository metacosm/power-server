package net.laprun.sustainability.power.measure;

import java.util.List;

import net.laprun.sustainability.power.SensorMetadata;

public abstract class AbstractPowerMeasure implements PowerMeasure {
    private final SensorMetadata sensorMetadata;
    private final List<double[]> measures;

    protected AbstractPowerMeasure(SensorMetadata sensorMetadata, List<double[]> measures) {
        this.sensorMetadata = sensorMetadata;
        this.measures = measures;
    }

    @Override
    public List<double[]> measures() {
        return measures;
    }

    @Override
    public SensorMetadata metadata() {
        return sensorMetadata;
    }

    public int numberOfSamples() {
        return measures.size();
    }

    double[] getComponentData(int componentIndex) {
        return measures.parallelStream().mapToDouble(measure -> measure[componentIndex]).toArray();
    }
}
