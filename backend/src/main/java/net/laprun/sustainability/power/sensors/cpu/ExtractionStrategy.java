package net.laprun.sustainability.power.sensors.cpu;

import java.util.Map;
import java.util.Set;

public interface ExtractionStrategy {
    Map<String, Double> cpuSharesFor(Set<String> pids);
}
