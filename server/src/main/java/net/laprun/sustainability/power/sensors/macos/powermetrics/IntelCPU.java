package net.laprun.sustainability.power.sensors.macos.powermetrics;

import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.CPU_SHARE;
import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.PACKAGE;

import java.util.HashMap;
import java.util.Map;

import net.laprun.sustainability.power.SensorMetadata;

class IntelCPU extends CPU {

  private static final SensorMetadata.ComponentMetadata packageComponent = new SensorMetadata.ComponentMetadata(
      PACKAGE, 0, "Intel energy model derived package power (CPUs+GT+SA)", true, "W");
  private static final SensorMetadata.ComponentMetadata cpuShareComponent = new SensorMetadata.ComponentMetadata(
      CPU_SHARE, 1, "Computed share of CPU", false, "decimal percentage");

  @Override
  public boolean doneExtractingPowerComponents(String line, HashMap<String, Number> powerComponents) {
    // line should look like: Intel energy model derived package power (CPUs+GT+SA): 8.53W
    final var powerIndex = line.indexOf("Intel ");
    if (powerIndex >= 0) {
      final var powerStartIndex = line.indexOf(':') + 1;
      final float value;
      try {
        value = Float.parseFloat(line.substring(powerStartIndex, line.indexOf('W')));
      } catch (Exception e) {
        throw new IllegalStateException("Cannot parse power value from line '" + line + "'", e);
      }
      powerComponents.put(PACKAGE, value);
      return true;
    }

    return false;
  }

  @Override
  boolean doneAfterComponentsInitialization(Map<String, SensorMetadata.ComponentMetadata> components) {
    components.put(PACKAGE, packageComponent);
    components.put(CPU_SHARE, cpuShareComponent);
    return true;
  }
}
