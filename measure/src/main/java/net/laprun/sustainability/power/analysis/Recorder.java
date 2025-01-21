package net.laprun.sustainability.power.analysis;

import java.util.stream.DoubleStream;

public interface Recorder {
    DoubleStream measures();
}
