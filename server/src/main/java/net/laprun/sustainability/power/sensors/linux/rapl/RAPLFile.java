package net.laprun.sustainability.power.sensors.linux.rapl;

import java.nio.file.Path;

interface RAPLFile {
  long extractEnergyInMicroJoules();

  static RAPLFile createFrom(Path file) {
    return ByteBufferRAPLFile.createFrom(file);
  }
}
