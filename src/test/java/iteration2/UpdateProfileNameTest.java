package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                Arguments.of(null, HttpStatus.SC_BAD_REQUEST));
    }

    @Test
    public void userCanChangeNameWhenValidData() {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String userName = UUID.randomUUID().toString().substring(0,8);

        String createUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, userName);

        //создаем пользователя и тащим токен
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .response();

        String userAuthToken = response.header("Authorization");

        String requestBody = """
                {
                   "name": "Nikolay Sysoev"
                 }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        String nameAfterChange = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .extract()
                .body()
                .path("name");

        assertEquals("Nikolay Sysoev", nameAfterChange);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void UserCanNotChangeNameWhenInvalidData(Object givenName, int statusCode){
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String userName = UUID.randomUUID().toString().substring(0,8);

        String createUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, userName);

        //создаем пользователя и тащим токен
        Response response = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .response();

        String userAuthToken = response.header("Authorization");
        String initialName = response.path("name");

        String requestBody = String.format("""
                {
                   "name": "%s"
                 }
                """, givenName);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .statusCode(statusCode);

        String NameAfterChange = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .extract()
                .body()
                .path("name");

        assertEquals(initialName, NameAfterChange);
    }
}
