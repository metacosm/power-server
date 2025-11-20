package net.laprun.sustainability.power.sensors.cpu;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zaxxer.nuprocess.NuProcessBuilder;

import io.quarkus.logging.Log;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import net.laprun.sustainability.power.nuprocess.BaseProcessHandler;

public class PSExtractionStrategy implements ExtractionStrategy {
    public static final PSExtractionStrategy INSTANCE = new PSExtractionStrategy();
    private final int fullCPU = CpuCoreSensor.availableProcessors() * 100;// each core contributes 100%
    private long lastUpdate = System.currentTimeMillis();

    @Override
    public Map<String, Double> cpuSharesFor(Set<String> pids) {
        if (pids.isEmpty()) {
            return Collections.emptyMap();
        }
        final var cpuShares = new HashMap<String, Double>(pids.size());
        final var pidList = String.join(",", pids);
        final var cmd = new String[] { "ps", "-p", pidList, "-o", "pid=,pcpu=" };
        final var psHandler = new BaseProcessHandler(cmd) {

            @Override
            public void onStdout(ByteBuffer buffer, boolean closed) {
                if (buffer.hasRemaining()) {
                    var bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    extractCPUSharesInto(bytes, cpuShares);
                }
            }

            @Override
            public void onExit(int statusCode) {
                final var now = System.currentTimeMillis();
                Log.debugf("ps called again after: %d", now - lastUpdate);
                lastUpdate = now;
                if (statusCode != 0) {
                    Log.warnf("Failed to extract CPU shares for pids: %s", pids);
                    cpuShares.clear();
                }
            }
        };
        new NuProcessBuilder(psHandler, psHandler.command()).run();
        return cpuShares;
    }

    void extractCPUSharesInto(byte[] bytes, Map<String, Double> cpuShares) {
        // todo: avoid creating a string?
        extractCPUSharesInto(new String(bytes), cpuShares);
    }

    void extractCPUSharesInto(String input, Map<String, Double> cpuShares) {
        final var lines = input.split("\n");
        for (var line : lines) {
            if (line.isBlank()) {
                continue;
            }
            extractCPUShare(line, cpuShares);
        }
    }

    private void extractCPUShare(String line, Map<String, Double> cpuShares) {
        try {
            line = line.trim();
            int spaceIndex = line.indexOf(' ');
            if (spaceIndex == -1) {
                // replace all space characters, if present, by a single white space
                line = line.replaceAll("\\s", " ");
                spaceIndex = line.indexOf(' ');
                if (spaceIndex == -1) {
                    Log.warn("Invalid CPU share line format: " + line);
                    return;
                }
            }

            var pid = line.substring(0, spaceIndex).trim();
            var cpuPercentage = line.substring(spaceIndex + 1).trim();
            final var value = Double.parseDouble(cpuPercentage) / fullCPU();
            Log.debugf("pid: %s -> cpu: %s/%d%% = %3.2f", pid, cpuPercentage, fullCPU(), value);
            if (value < 0) {
                Log.warnf("Invalid CPU share percentage: %s", cpuPercentage);
                return;
            }
            final var previous = cpuShares.put(pid, value);
            if (previous != null) {
                Log.warnf("Duplicated pid entry: %s. Replaced previous value %s by new value %s", pid, previous, value);
            }
        } catch (NumberFormatException e) {
            Log.warnf("Failed to parse CPU percentage for line: '%s', cause: %s", line, e);
        }
    }

    int fullCPU() {
        return fullCPU;
    }
}
