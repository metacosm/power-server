package io.github.metacosm;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class PowerResourceTest {

    @Test
    public void testHelloEndpoint() {
        final var pid = ProcessHandle.current().pid();
        given()
                .when().get("/power/" + pid)
                .then()
                .statusCode(200);
    }

}