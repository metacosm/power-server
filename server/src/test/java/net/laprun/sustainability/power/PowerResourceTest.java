package net.laprun.sustainability.power;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PowerResourceTest {

    @Test
    public void testPowerEndpoint() {
        final var pid = getPid();
        given()
                .when().get("/power/" + pid)
                .then()
                .statusCode(200);
    }

    protected long getPid() {
        return ProcessHandle.current().pid();
    }

    @Test
    @EnabledOnOs(OS.MAC)
    @EnabledIfSystemProperty(named = "os.arch", matches = "aarch64")
    public void testMacOSAppleSiliconMetadataEndpoint() {
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
        assertEquals(SensorUnit.mW, cpu.unit());
        assertEquals(SensorUnit.W, cpu.unit().base());
        assertTrue(cpu.isAttributed());

        final var cpuShare = metadata.metadataFor("cpuShare");
        assertEquals(3, cpuShare.index());
        assertEquals("cpuShare", cpuShare.name());
        assertEquals(SensorUnit.decimalPercentage, cpuShare.unit());
        assertFalse(cpuShare.isAttributed());
    }

    @Test
    @EnabledOnOs(OS.MAC)
    @EnabledIfSystemProperty(named = "os.arch", matches = "x86_64")
    public void testMacOSIntelMetadataEndpoint() {
        final var metadata = given()
                .when().get("/power/metadata")
                .then()
                .statusCode(200)
                .extract().body().as(SensorMetadata.class);
        assertEquals(2, metadata.componentCardinality());
        assertTrue(metadata.documentation().contains("powermetrics"));
        assertTrue(metadata.components().keySet().containsAll(Set.of("Package", "cpuShare")));

        final var cpu = metadata.metadataFor("Package");
        assertEquals(0, cpu.index());
        assertEquals(SensorUnit.W, cpu.unit());
        assertTrue(cpu.isAttributed());

        final var cpuShare = metadata.metadataFor("cpuShare");
        assertEquals(1, cpuShare.index());
        assertEquals("cpuShare", cpuShare.name());
        assertEquals(SensorUnit.decimalPercentage, cpuShare.unit());
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
