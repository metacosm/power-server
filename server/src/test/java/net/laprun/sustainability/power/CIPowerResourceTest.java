package net.laprun.sustainability.power;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CIQuarkusTestProfile.class) // only activated when quarkus.test.profile.tags='ci', uses @Mock annotated beans
public class CIPowerResourceTest extends PowerResourceTest {

    protected long getPid() {
        return 29419;
    }

    @Override
    public void testLinuxMetadataEndpoint() {
        // overridden to disable as with the profile activation, the MockPowerSensor implementation is picked up, which is a macOS-specific implementation
    }

    @Test
    public void testDBBasedEndpoint() {
        final var pid = getPid();
        given()
                .when().post("/power/start/cipowerresourcetest/" + pid)
                .then()
                .statusCode(204);
    }
}
