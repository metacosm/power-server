package io.github.metacosm.power;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CIQuarkusTestProfile.class)
public class CIPowerResourceTest {

    protected long getPid() {
        return 29419;
    }

    @Test
    public void testPowerEndpoint() {
        final var pid = getPid();
        given()
                .when().get("/power/" + pid)
                .then()
                .statusCode(200);
    }

    @Test
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
        assertEquals("mW", cpu.unit());
        assertTrue(cpu.isAttributed());

        final var cpuShare = metadata.metadataFor("cpuShare");
        assertEquals(3, cpuShare.index());
        assertEquals("cpuShare", cpuShare.name());
        assertEquals("decimal percentage", cpuShare.unit());
        assertFalse(cpuShare.isAttributed());
    }
}
