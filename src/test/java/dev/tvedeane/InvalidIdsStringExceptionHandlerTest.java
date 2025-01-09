package dev.tvedeane;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

@MicronautTest
public class InvalidIdsStringExceptionHandlerTest {
    @Test
    public void returnsMessageOnWrongIdsInput(RequestSpecification spec) {
        spec
            .when()
            .get("/boardgames/stream/a")
            .then()
            .statusCode(400)
            .body(is("Integers separated by commas expected"));
    }
}
