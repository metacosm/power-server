package net.laprun.sustainability.power.sensors;

import java.util.HashSet;
import java.util.Set;

public class PIDRegistry {
    private final Set<String> pids = new HashSet<>();

    public void register(RegisteredPID registeredPID) {
        if (!RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID.equals(registeredPID)) {
            pids.add(registeredPID.pidAsString());
        }
    }

    public void unregister(RegisteredPID registeredPID) {
        if (!RegisteredPID.SYSTEM_TOTAL_REGISTERED_PID.equals(registeredPID)) {
            pids.remove(registeredPID.pidAsString());
        }
    }

    public Set<String> pids() {
        return pids;
    }
}
