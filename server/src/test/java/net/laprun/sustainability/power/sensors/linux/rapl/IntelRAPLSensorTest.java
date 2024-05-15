package net.laprun.sustainability.power.sensors.linux.rapl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.laprun.sustainability.power.SensorMetadata;

public class IntelRAPLSensorTest {
  @Test
  void checkMetadata() {
    var metadata = loadMetadata("rapl/intel-rapl_1/energy_uj", "rapl/intel-rapl_2/energy_uj");
    assertEquals(4, metadata.componentCardinality());
    checkComponent(metadata, "CPU", 0);
    checkComponent(metadata, "GPU", 1);
    checkComponent(metadata, "CPU_uj", 2);
    checkComponent(metadata, "GPU_uj", 3);
  }

  private SensorMetadata loadMetadata(String... fileNames) {
    final var files = Arrays.stream(fileNames)
        .map(name -> new File(getClass().getClassLoader().getResource(name).getFile()).getAbsolutePath())
        .toArray(String[]::new);
    return new IntelRAPLSensor(files).metadata();
  }

  private static void checkComponent(SensorMetadata metadata, String name, int index) {
    // check string instead of constants to ensure "API" compatibility as these keys will be published
    final var component = metadata.metadataFor(name);
    assertEquals(name, component.name());
    assertEquals(index, component.index());
  }
}
