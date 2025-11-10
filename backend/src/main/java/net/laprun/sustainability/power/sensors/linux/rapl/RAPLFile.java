package net.laprun.sustainability.power.sensors.linux.rapl;

import java.nio.file.Path;

interface RAPLFile {
    long extractEnergyInMicroJoules();

    String contentAsString();

    static RAPLFile createFrom(Path file) {
        // assume that file is readable in test mode, so run without sudo
        // note that this will fail in a realistic environment
        return new ProcessReadRAPLFile(file, !TestMode.enabled);
    }
}
