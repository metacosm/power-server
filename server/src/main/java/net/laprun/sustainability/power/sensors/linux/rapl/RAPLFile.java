package net.laprun.sustainability.power.sensors.linux.rapl;

import java.nio.file.Path;

interface RAPLFile {
    long extractPowerMeasure();

    static RAPLFile createFrom(Path file) {
        return ByteBufferRAPLFile.createFrom(file);
    }
}
