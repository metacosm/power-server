package net.laprun.sustainability.power.sensors.macos.powermetrics;

import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.CPU_SHARE;
import static net.laprun.sustainability.power.sensors.macos.powermetrics.MacOSPowermetricsSensor.GPU;

import java.util.Map;

import net.laprun.sustainability.power.SensorMetadata;
import net.laprun.sustainability.power.sensors.RegisteredPID;

class ProcessRecord {
    final double cpu;
    final double gpu;
    final String pid;

    double[] asAttributedMeasure(SensorMetadata metadata, Map<String, Number> powerComponents, final double totalSampledCPU,
            final double totalSampledGPU) {
        final var cpuShare = cpu / totalSampledCPU;
        final var measure = new double[metadata.componentCardinality()];

        metadata.components().forEach((name, cm) -> {
            final var index = cm.index();
            final var value = CPU_SHARE.equals(name) ? cpuShare : powerComponents.getOrDefault(name, 0).doubleValue();

            if (cm.isAttributed()) {
                final double attributionFactor;
                if (GPU.equals(name)) {
                    attributionFactor = totalSampledGPU != 0 ? gpu / totalSampledGPU : 0.0;
                } else {
                    attributionFactor = cpuShare;
                }
                measure[index] = value * attributionFactor;
            } else {
                measure[index] = value;
            }
        });

        return measure;
    }

    public ProcessRecord(String line) throws IllegalArgumentException {
        // Expected normal output:
        //Name                               ID     CPU ms/s  samp ms/s  User%  Deadlines (<2 ms, 2-5 ms)  Wakeups (Intr, Pkg idle)  GPU ms/s
        //iTerm2                             1008   46.66     46.91      83.94  0.00    0.00               30.46   0.00              0.00
        // Expected summary output:
        //Name                               ID     CPU ms/s  samp ms/s [total]     User%  Deadlines/s [total] (<2 ms, 2-5 ms)  Wakeups/s [total] (Intr, Pkg idle)  Dead  GPU ms/s
        //WindowServer                       406    493.74    493.96    [5165.88  ] 64.82  65.95   [690    ] 0.00    [0      ]  656.62  [6870   ] 4.21    [44     ] N     0.00
        try {
            // Trim leading/trailing whitespace
            line = line.trim();

            // Find first whitespace block after process name (marks start of ID)
            int idStart = findFirstWhitespace(line);
            if (idStart == -1) {
                throw new IllegalArgumentException("Cannot find ID in line: " + line);
            }

            // Skip whitespace to get to ID
            idStart = skipWhitespace(line, idStart);
            int idEnd = findNextWhitespace(line, idStart);
            pid = RegisteredPID.prepare(line.substring(idStart, idEnd));

            // Skip CPU ms/s column (skip whitespace, then number, then whitespace)
            int pos = skipWhitespace(line, idEnd);
            pos = skipNumber(line, pos);

            // Now at samp ms/s
            pos = skipWhitespace(line, pos);
            int sampStart = pos;
            int sampEnd = skipNumber(line, sampStart);
            cpu = Double.parseDouble(line.substring(sampStart, sampEnd));

            // Skip to end and work backwards to find GPU ms/s
            // The GPU value is the last numeric value on the line
            int lastNumEnd = line.length();
            while (lastNumEnd > 0 && Character.isWhitespace(line.charAt(lastNumEnd - 1))) {
                lastNumEnd--;
            }

            int lastNumStart = lastNumEnd;
            while (lastNumStart > 0 && isNumberChar(line.charAt(lastNumStart - 1))) {
                lastNumStart--;
            }

            if (lastNumStart < lastNumEnd) {
                gpu = Double.parseDouble(line.substring(lastNumStart, lastNumEnd));
            } else {
                throw new IllegalArgumentException("Cannot find GPU value in line: " + line);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Received line doesn't conform to expected format: " + line, e);
        }
    }

    private static int findFirstWhitespace(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int skipWhitespace(String line, int pos) {
        while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int findNextWhitespace(String line, int pos) {
        while (pos < line.length() && !Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int skipNumber(String line, int pos) {
        while (pos < line.length() && isNumberChar(line.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == '-' || c == 'e' || c == 'E';
    }

    @Override
    public String toString() {
        return "ProcessRecord (pid:" + pid + ") -> cpu=" + cpu + ", gpu=" + gpu;
    }
}
