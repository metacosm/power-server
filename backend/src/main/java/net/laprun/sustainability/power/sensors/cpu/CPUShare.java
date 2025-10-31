package net.laprun.sustainability.power.sensors.cpu;

import java.util.Map;
import java.util.Set;

public enum CPUShare {
    ;

    public static Map<String, Double> cpuSharesFor(Set<String> pids) {
        return cpuSharesFor(pids, PSExtractionStrategy.INSTANCE);
    }

    public static Map<String, Double> cpuSharesFor(Set<String> pids, ExtractionStrategy strategy) {
        return strategy.cpuSharesFor(pids);
    }
}
