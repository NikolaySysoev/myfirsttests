package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UpdateProfileNameTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> invalidName() {
        return Stream.of(
                Arguments.of("Nikolay", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("Nikolay Nikolay Nikolay", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("Nikolay123 Sysoev", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("Nikolay Sysoev123", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("Nikolay^&*(! Sysoev", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("Nikolay Sysoev^&*(!", HttpStatus.SC_BAD_REQUEST),
                Arguments.of("12312 ^&*(!", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(null, HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @Test
    public void userCanChangeNameWhenValidData() {
        String requestBody = """
                {
                   "name": "Nikolay Sysoev"
                 }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void UserCanNotChangeNameWhenInvalidData(Object name, int statusCode){
        String requestBody = String.format("""
                {
                   "name": "%s"
                 }
                """, name);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(statusCode);
    }
}
