package io.github.metacosm.power.sensors.macos.powermetrics;

import io.github.metacosm.power.SensorMetadata;

import java.util.HashMap;
import java.util.Map;

import static io.github.metacosm.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.*;

class AppleSiliconCPU extends io.github.metacosm.power.sensors.macos.powermetrics.CPU {
    private static final SensorMetadata.ComponentMetadata cpuComponent = new SensorMetadata.ComponentMetadata(CPU, 0, "CPU power", true, "mW");
    private static final SensorMetadata.ComponentMetadata gpuComponent = new SensorMetadata.ComponentMetadata(GPU, 1, "GPU power", true, "mW");
    private static final SensorMetadata.ComponentMetadata aneComponent = new SensorMetadata.ComponentMetadata(ANE, 2, "Apple Neural Engine power", false, "mW");
    private static final SensorMetadata.ComponentMetadata cpuShareComponent = new SensorMetadata.ComponentMetadata(CPU_SHARE, 3, "Computed share of CPU", false, "decimal percentage");
    private static final String COMBINED = "Combined";

    public AppleSiliconCPU() {

    }

    @Override
    public void addComponentIfFound(String line, Map<String, SensorMetadata.ComponentMetadata> components) {
        // looking for line fitting the: "<name> Power: xxx mW" pattern, where "name" will be a considered metadata component
        final var powerIndex = line.indexOf(" Power");
        // lines with `-` as the second char are disregarded as of the form: "E-Cluster Power: 6 mW" which fits the metadata pattern but shouldn't be considered
        if (powerIndex >= 0 && '-' != line.charAt(1)) {
            addComponentTo(line.substring(0, powerIndex), components);
        }
    }

    private static void addComponentTo(String name, Map<String, SensorMetadata.ComponentMetadata> components) {
        switch (name) {
            case CPU, GPU, ANE:
                // already pre-added
                break;
            case COMBINED:
                // should be ignored
                break;
            default:
                components.put(name, new SensorMetadata.ComponentMetadata(name, components.size(), name, false, "mW"));
        }
    }

    @Override
    public void extractPowerComponents(String line, HashMap<String, Integer> powerComponents) {

    }

    @Override
    void initComponents(Map<String, SensorMetadata.ComponentMetadata> components) {
        // init map with known components
        components.put(MacOSPowermetricsSensor.CPU, cpuComponent);
        components.put(GPU, gpuComponent);
        components.put(ANE, aneComponent);
        components.put(CPU_SHARE, cpuShareComponent);
    }
}
