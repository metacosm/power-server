package net.laprun.sustainability.power.analysis;

import java.util.stream.DoubleStream;

public interface Recorder {
    default DoubleStream measures() {
        return DoubleStream.of(liveMeasures());
    }

    double[] liveMeasures();
}
