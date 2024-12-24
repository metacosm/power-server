package net.laprun.sustainability.power;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SensorUnitTest {

    @Test
    void ofShouldWork() {
        SensorUnit su = SensorUnit.of("mW");
        assertThat(su.symbol()).isEqualTo("mW");
        assertThat(su.base()).isEqualTo(SensorUnit.W);
        assertThat(su.factor()).isEqualTo(1e-3);

        su = SensorUnit.of("decimal percentage");
        assertThat(su).isEqualTo(SensorUnit.decimalPercentage);
        assertThat(su.factor()).isEqualTo(1);
        assertThat(su.base()).isEqualTo(SensorUnit.decimalPercentage);

        su = SensorUnit.of("dW");
        assertThat(su.base()).isEqualTo(SensorUnit.W);
        assertThat(su.factor()).isEqualTo(0.1);
    }

    @Test
    void blankSymbolShouldFail() {
        final var e = assertThrows(NullPointerException.class, () -> SensorUnit.of(null));
        assertThat(e.getMessage()).contains("Unit symbol cannot be null");

        final var e2 = assertThrows(IllegalArgumentException.class, () -> SensorUnit.of("    "));
        assertThat(e2.getMessage()).contains("Unit symbol cannot be blank");
    }

    @Test
    void unknownPrefixShouldFail() {
        final var e = assertThrows(IllegalArgumentException.class, () -> SensorUnit.of("pW"));
        assertThat(e.getMessage()).contains("Unknown unit prefix 'p'");
    }

    @Test
    void isCommensurableWithShouldWork() {
        assertThat(SensorUnit.of("mW").isCommensurableWith(SensorUnit.W)).isTrue();
        assertThat(SensorUnit.of("mW").isCommensurableWith(SensorUnit.J)).isFalse();
    }

    @Test
    void conversionFactorShouldWork() {
        assertEquals(1, SensorUnit.W.conversionFactorTo(SensorUnit.W));
        assertEquals(1, SensorUnit.J.conversionFactorTo(SensorUnit.J));
        assertEquals(1, SensorUnit.decimalPercentage.conversionFactorTo(SensorUnit.decimalPercentage));
        assertEquals(1, SensorUnit.of("mW").conversionFactorTo(SensorUnit.of("mW")));
        assertEquals(0.001, SensorUnit.of("mW").conversionFactorTo(SensorUnit.W), 0.0001);
        assertEquals(1000, SensorUnit.of("W").conversionFactorTo(SensorUnit.of("mW")), 0.0001);
        assertEquals(1000, SensorUnit.of("mW").conversionFactorTo(SensorUnit.of("µW")), 0.0001);
        assertEquals(1e-3, SensorUnit.of("µW").conversionFactorTo(SensorUnit.of("mW")), 0.0001);
        assertEquals(0.1, SensorUnit.of("mW").conversionFactorTo(SensorUnit.of("cW")), 0.0001);
        assertEquals(10, SensorUnit.of("cW").conversionFactorTo(SensorUnit.of("mW")), 0.0001);
        assertEquals(1e6, SensorUnit.of("MW").conversionFactorTo(SensorUnit.W), 0.0001);
        assertEquals(1e-6, SensorUnit.W.conversionFactorTo(SensorUnit.of("MW")), 0.0001);
    }
}
