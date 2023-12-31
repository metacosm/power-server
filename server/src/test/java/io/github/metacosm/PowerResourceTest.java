package io.github.metacosm;

import io.github.metacosm.power.SensorMetadata;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class PowerResourceTest {

    @Test
    public void testPowerEndpoint() {
        final var pid = ProcessHandle.current().pid();
        given()
                .when().get("/power/" + pid)
                .then()
                .statusCode(200);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    public void testMacOSMetadataEndpoint() {
        final var metadata = given()
                .when().get("/power/metadata")
                .then()
                .statusCode(200)
                .extract().body().as(SensorMetadata.class);
        assertEquals(4, metadata.componentCardinality());
        assertTrue(metadata.documentation().contains("powermetrics"));
        assertTrue(metadata.components().keySet().containsAll(Set.of("CPU", "GPU", "ANE", "cpuShare")));

        final var cpu = metadata.metadataFor("CPU");
        assertEquals(0, cpu.index());
        assertEquals("CPU", cpu.name());
        assertEquals("mW", cpu.unit());
        assertTrue(cpu.isAttributed());

        final var cpuShare = metadata.metadataFor("cpuShare");
        assertEquals(3, cpuShare.index());
        assertEquals("cpuShare", cpuShare.name());
        assertEquals("decimal percentage", cpuShare.unit());
        assertFalse(cpuShare.isAttributed());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void testLinuxMetadataEndpoint() {
        final var metadata = given()
                .when().get("/power/metadata")
                .then()
                .statusCode(200)
                .extract().body().as(SensorMetadata.class);
        assertTrue(metadata.documentation().contains("RAPL"));
    }

}