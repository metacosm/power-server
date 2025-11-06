package net.laprun.sustainability.power;

import java.time.Duration;
import java.util.Optional;

public enum ProcessUtils {
    ;

    public static long validPIDOrFail(String pid) {
        final var parsedPID = Long.parseLong(pid);
        processHandleOf(parsedPID);
        return parsedPID;
    }

    public static ProcessHandle processHandleOf(long parsedPID) {
        return ProcessHandle.of(parsedPID).orElseThrow(() -> new IllegalArgumentException("Unknown process: " + parsedPID));
    }
}
