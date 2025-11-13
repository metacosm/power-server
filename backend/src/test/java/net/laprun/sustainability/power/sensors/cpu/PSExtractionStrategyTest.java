package net.laprun.sustainability.power.sensors.cpu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PSExtractionStrategyTest {
    private static final int fullCPU = PSExtractionStrategy.INSTANCE.fullCPU();

    @Test
    void extractCPUSharesInto_shouldParseValidSingleLine() {
        String input = "12345 25.5";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        checkCPUShare(cpuShares, "12345", 25.5);
    }

    private static void checkCPUShare(Map<String, Double> cpuShares, String pid, double cpuPercentageFromPS) {
        final var actual = cpuShares.get(pid);
        assertThat(actual).isLessThan(1.0);
        assertThat(actual).isEqualTo(cpuPercentageFromPS / fullCPU);
    }

    @Test
    void extractCPUSharesInto_shouldParseMultipleLines() {
        String input = "12345 25.5\n67890 15.3\n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
        checkCPUShare(cpuShares, "11111", 5.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleExtraWhitespace() {
        String input = "  12345   25.5  \n  67890   15.3  ";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
    }

    @Test
    void extractCPUSharesInto_shouldSkipEmptyLines() {
        String input = "12345 25.5\n\n67890 15.3\n   \n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
        checkCPUShare(cpuShares, "11111", 5.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleZeroCpuPercentage() {
        String input = "12345 0.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        checkCPUShare(cpuShares, "12345", 0.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleHighCpuPercentage() {
        String input = "12345 354.287";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        checkCPUShare(cpuShares, "12345", 354.287);
    }

    @Test
    void extractCPUSharesInto_shouldIgnoreInvalidLineSpace() {
        String input = "12345 25.5\n  invalidline   \n67890 15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        // Only valid lines should be parsed
        assertThat(cpuShares).hasSize(2);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
    }

    @Test
    void extractCPUSharesInto_shouldIgnoreInvalidNumberFormat() {
        String input = "12345 25.5\n67890 notanumber\n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        // Only valid lines should be parsed
        assertThat(cpuShares).hasSize(2);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "11111", 5.0);
        assertThat(cpuShares).doesNotContainKey("67890");
    }

    @Test
    void extractCPUSharesInto_shouldHandleEmptyInput() {
        String input = "";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldHandleOnlyWhitespace() {
        String input = "   \n  \n   ";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldOnlyKeepLatestValueOnDuplicatePids() {
        String input = "12345 25.5\n12345 30.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        checkCPUShare(cpuShares, "12345", 30.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleMultipleSpacesBetweenValues() {
        String input = "12345    25.5\n67890  15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
    }

    @Test
    void extractCPUSharesInto_shouldHandleTabCharacters() {
        String input = "12345\t25.5\n67890\t15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        checkCPUShare(cpuShares, "12345", 25.5);
        checkCPUShare(cpuShares, "67890", 15.3);
    }

    @Test
    void extractCPUSharesInto_shouldHandleRealPsOutput() {
        // Realistic output from ps command
        String input = "  1234  12.5\n  5678   3.2\n 91011  75.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        checkCPUShare(cpuShares, "1234", 12.5);
        checkCPUShare(cpuShares, "5678", 3.2);
        checkCPUShare(cpuShares, "91011", 75.0);
    }

    @Test
    void extractCPUSharesInto_shouldIgnorNegativeCPUShares() {
        String input = "12345 -5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldHandleScientificNotation() {
        String input = "12345 1.5e2";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        checkCPUShare(cpuShares, "12345", 150.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleLineWithOnlyPid() {
        String input = "12345";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.INSTANCE.extractCPUSharesInto(input, cpuShares);

        // Line without space should be ignored
        assertThat(cpuShares).isEmpty();
    }
}
