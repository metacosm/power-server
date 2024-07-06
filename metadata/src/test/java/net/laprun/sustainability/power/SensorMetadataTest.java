package net.laprun.sustainability.power;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SensorMetadataTest {
    @Test
    void shouldFailIfTotalComponentsAreOutOfRange() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(Collections.emptyMap(), "", new int[] { 0, 1, -1 }));
        final var message = e.getMessage();
        assertTrue(message.contains("0"));
        assertTrue(message.contains("1"));
        assertTrue(message.contains("-1"));
    }

    @Test
    void shouldFailIfTotalComponentsAreNotCommensurateToWatts() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(
                        Map.of("foo", new SensorMetadata.ComponentMetadata("foo", 0, "", false, SensorUnit.decimalPercentage)),
                        "", new int[] { 0, 1, -1 }));
        final var message = e.getMessage();
        assertTrue(message.contains("1"));
        assertTrue(message.contains("-1"));
        assertTrue(message.contains("foo"));
    }

    @Test
    void shouldFailIfNoTotalComponentsAreProvided() {
        final var e = assertThrows(NullPointerException.class,
                () -> new SensorMetadata(Collections.emptyMap(), "", null));
        final var message = e.getMessage();
        assertTrue(message.contains("Must provide total components"));
    }

    @Test
    void shouldFailIfNoComponentsAreProvided() {
        final var e = assertThrows(NullPointerException.class,
                () -> new SensorMetadata(null, "", null));
        final var message = e.getMessage();
        assertTrue(message.contains("Must provide components"));
    }
}
