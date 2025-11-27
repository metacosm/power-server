package net.laprun.sustainability.power.sensors;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PIDRegistry {
    private final Set<RegisteredPID> pids = new HashSet<>();

    public void register(RegisteredPID registeredPID) {
        if (!RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID.equals(registeredPID)) {
            pids.add(registeredPID);
        }
    }

    public void unregister(RegisteredPID registeredPID) {
        if (!RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID.equals(registeredPID)) {
            pids.remove(registeredPID);
        }
    }

    public Set<String> pidsAsStrings() {
        return pids.stream().map(RegisteredPID::pidAsString).collect(Collectors.toSet());
    }

    public Set<RegisteredPID> pids() {
        return pids;
    }

    public int size() {
        return pids.size();
    }
}
