package net.laprun.sustainability.power.sensors.cpu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PSExtractionStrategyTest {

    @Test
    void extractCPUSharesInto_shouldParseValidSingleLine() {
        String input = "12345 25.5";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
    }

    @Test
    void extractCPUSharesInto_shouldParseMultipleLines() {
        String input = "12345 25.5\n67890 15.3\n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
        assertThat(cpuShares.get("11111")).isEqualTo(5.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleExtraWhitespace() {
        String input = "  12345   25.5  \n  67890   15.3  ";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
    }

    @Test
    void extractCPUSharesInto_shouldSkipEmptyLines() {
        String input = "12345 25.5\n\n67890 15.3\n   \n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
        assertThat(cpuShares.get("11111")).isEqualTo(5.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleZeroCpuPercentage() {
        String input = "12345 0.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        assertThat(cpuShares.get("12345")).isEqualTo(0.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleHighCpuPercentage() {
        String input = "12345 354.287";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        assertThat(cpuShares.get("12345")).isEqualTo(354.287);
    }

    @Test
    void extractCPUSharesInto_shouldIgnoreInvalidLineSpace() {
        String input = "12345 25.5\n  invalidline   \n67890 15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        // Only valid lines should be parsed
        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
    }

    @Test
    void extractCPUSharesInto_shouldIgnoreInvalidNumberFormat() {
        String input = "12345 25.5\n67890 notanumber\n11111 5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        // Only valid lines should be parsed
        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("11111")).isEqualTo(5.0);
        assertThat(cpuShares).doesNotContainKey("67890");
    }

    @Test
    void extractCPUSharesInto_shouldHandleEmptyInput() {
        String input = "";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldHandleOnlyWhitespace() {
        String input = "   \n  \n   ";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldOnlyKeepLatestValueOnDuplicatePids() {
        String input = "12345 25.5\n12345 30.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        assertThat(cpuShares.get("12345")).isEqualTo(30.0); // only newest value is kept
    }

    @Test
    void extractCPUSharesInto_shouldHandleMultipleSpacesBetweenValues() {
        String input = "12345    25.5\n67890  15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
    }

    @Test
    void extractCPUSharesInto_shouldHandleTabCharacters() {
        String input = "12345\t25.5\n67890\t15.3";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
        assertThat(cpuShares.get("67890")).isEqualTo(15.3);
    }

    @Test
    void extractCPUSharesInto_shouldHandleRealPsOutput() {
        // Realistic output from ps command
        String input = "  1234  12.5\n  5678   3.2\n 91011  75.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(3);
        assertThat(cpuShares.get("1234")).isEqualTo(12.5);
        assertThat(cpuShares.get("5678")).isEqualTo(3.2);
        assertThat(cpuShares.get("91011")).isEqualTo(75.0);
    }

    @Test
    void extractCPUSharesInto_shouldIgnorNegativeCPUShares() {
        String input = "12345 -5.0";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).isEmpty();
    }

    @Test
    void extractCPUSharesInto_shouldAddToExistingMap() {
        Map<String, Double> cpuShares = new HashMap<>();
        cpuShares.put("99999", 50.0);

        String input = "12345 25.5";
        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(2);
        assertThat(cpuShares.get("99999")).isEqualTo(50.0);
        assertThat(cpuShares.get("12345")).isEqualTo(25.5);
    }

    @Test
    void extractCPUSharesInto_shouldHandleScientificNotation() {
        String input = "12345 1.5e2";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        assertThat(cpuShares).hasSize(1);
        assertThat(cpuShares.get("12345")).isEqualTo(150.0);
    }

    @Test
    void extractCPUSharesInto_shouldHandleLineWithOnlyPid() {
        String input = "12345";
        Map<String, Double> cpuShares = new HashMap<>();

        PSExtractionStrategy.extractCPUSharesInto(input, cpuShares);

        // Line without space should be ignored
        assertThat(cpuShares).isEmpty();
    }
}
