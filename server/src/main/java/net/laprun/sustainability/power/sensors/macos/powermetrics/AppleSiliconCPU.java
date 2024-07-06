package net.laprun.sustainability.power.sensors.macos.powermetrics;

import static net.laprun.sustainability.power.SensorUnit.*;
import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.laprun.sustainability.power.SensorMetadata;

class AppleSiliconCPU extends CPU {
    private static final List<Integer> defaultTotalComponents = List.of(0, 1, 2);
    private static final SensorMetadata.ComponentMetadata cpuComponent = new SensorMetadata.ComponentMetadata(CPU, 0,
            "CPU power", true, mW);
    private static final SensorMetadata.ComponentMetadata gpuComponent = new SensorMetadata.ComponentMetadata(GPU, 1,
            "GPU power", true, mW);
    private static final SensorMetadata.ComponentMetadata aneComponent = new SensorMetadata.ComponentMetadata(ANE, 2,
            "Apple Neural Engine power", false, mW);
    private static final SensorMetadata.ComponentMetadata cpuShareComponent = new SensorMetadata.ComponentMetadata(CPU_SHARE, 3,
            "Computed share of CPU", false, decimalPercentage);
    private static final String COMBINED = "Combined";
    private static final String POWER_INDICATOR = " Power: ";
    private static final int POWER_INDICATOR_LENGTH = POWER_INDICATOR.length();
    private final List<Integer> totalComponents = new ArrayList<>();

    public AppleSiliconCPU() {
    }

    @Override
    int[] getTotalComponents() {
        return totalComponents.stream().mapToInt(i -> i).toArray();
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

    private void addComponentTo(String name, Map<String, SensorMetadata.ComponentMetadata> components) {
        switch (name) {
            case CPU, GPU, ANE:
                // already pre-added
                break;
            case COMBINED:
                // should be ignored
                break;
            default:
                final var index = components.size();
                components.put(name, new SensorMetadata.ComponentMetadata(name, index, name, false, mW));
                totalComponents.add(index);
        }
    }

    @Override
    public boolean doneExtractingPowerComponents(String line, HashMap<String, Number> powerComponents) {
        // looking for line fitting the: "<name> Power: xxx mW" pattern and add all of the associated values together
        final var powerIndex = line.indexOf(POWER_INDICATOR);
        // lines with `-` as the second char are disregarded as of the form: "E-Cluster Power: 6 mW" which fits the pattern but shouldn't be considered
        // also ignore Combined Power if available since it is the sum of the other components
        if (powerIndex >= 0 && '-' != line.charAt(1) && !line.startsWith("Combined")) {
            // get component name
            final var name = line.substring(0, powerIndex);
            // extract power value
            final int value;
            try {
                value = Integer.parseInt(line.substring(powerIndex + POWER_INDICATOR_LENGTH, line.indexOf('m') - 1));
            } catch (Exception e) {
                throw new IllegalStateException("Cannot parse power value from line '" + line + "'", e);
            }
            powerComponents.put(name, value);
        }

        // we break out once we 've found all the extracted components (in this case, only cpuShare is not extracted)
        return powerComponents.size() == metadata().componentCardinality() - 1;
    }

    @Override
    boolean doneAfterComponentsInitialization(Map<String, SensorMetadata.ComponentMetadata> components) {
        // init map with known components
        components.put(MacOSPowermetricsSensor.CPU, cpuComponent);
        components.put(GPU, gpuComponent);
        components.put(ANE, aneComponent);
        components.put(CPU_SHARE, cpuShareComponent);
        totalComponents.addAll(defaultTotalComponents);
        return false;
    }
}
