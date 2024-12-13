package net.laprun.sustainability.power;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class SensorMetadataTest {

    @Test
    void shouldFailIfTotalComponentsAreOutOfRange() {
        final var name = "comp0";
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(
                        List.of(new SensorMetadata.ComponentMetadata(name, -1, null, true, SensorUnit.W, true)), ""));
        final var message = e.getMessage();
        assertTrue(message.contains("range"));
        assertTrue(message.contains("-1"));
    }

    @Test
    void shouldFailIfComponentHasNullName() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(
                        List.of(new SensorMetadata.ComponentMetadata(null, -1, null, true, (SensorUnit) null, true)), ""));
        final var message = e.getMessage();
        assertTrue(message.contains("Component name cannot be null"));
    }

    @Test
    void shouldFailIfComponentHasNullUnit() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(
                        List.of(new SensorMetadata.ComponentMetadata("invalid", -1, null, true, (SensorUnit) null, true)), ""));
        final var message = e.getMessage();
        assertTrue(message.contains("Component unit cannot be null"));
    }

    @Test
    void shouldFailOnDuplicatedComponentNames() {
        final var name = "component";
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(List.of(
                        new SensorMetadata.ComponentMetadata(name, 0, null, true, "mW", false),
                        new SensorMetadata.ComponentMetadata(name, 1, null, true, "mW", false)), ""));
        final var message = e.getMessage();
        assertTrue(message.contains(name) && message.contains("0") && message.contains("1"));
    }

    @Test
    void shouldFailIfComponentsDoNotCoverFullRange() {
        final var e = assertThrows(IllegalArgumentException.class,
                () -> new SensorMetadata(List.of(
                        new SensorMetadata.ComponentMetadata("foo", 0, null, true, "mW", false),
                        new SensorMetadata.ComponentMetadata("component2", 0, null, true, "mW", false)), ""));
        final var message = e.getMessage();
        assertTrue(message.contains("Multiple components are using index 0"));
        assertTrue(message.contains("foo"));
        assertTrue(message.contains("component2"));
        assertTrue(message.contains("Missing indices: {1}"));
    }

    @Test
    void shouldFailIfNoComponentsAreProvided() {
        final var e = assertThrows(NullPointerException.class,
                () -> new SensorMetadata(null, ""));
        final var message = e.getMessage();
        assertTrue(message.contains("Must provide components"));
    }
}
