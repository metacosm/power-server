package net.laprun.sustainability.power.sensors.linux.rapl;

import java.nio.file.Path;

interface RAPLFile {
    long extractEnergyInMicroJoules();

    String contentAsString();

    static RAPLFile createFrom(Path file) {
        return new ProcessReadRAPLFile(file);
    }
}
