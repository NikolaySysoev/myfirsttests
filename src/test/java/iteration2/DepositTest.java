package iteration2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(4999.99),
                Arguments.of(0.01),
                Arguments.of(5000.00));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(0.00, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(5000.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-0.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of("100", HttpStatus.SC_INTERNAL_SERVER_ERROR),
                Arguments.of(null, HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    public static Stream<Arguments> depositInvalidAccount() {
        return Stream.of(
                Arguments.of(3, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999999, HttpStatus.SC_BAD_REQUEST));
    }

    @ParameterizedTest
    @MethodSource("depositValidData")
    public void userCanDepositOnHisAccount(double balance) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' }.balance");

        String requestBody = String.format("""
                {
                    "id": 1,
                        "balance": %s
                }
                """, balance);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Float balanceAfterDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' }.balance");

        assertEquals(initialBalance + balance, balanceAfterDeposit, 0.01);
    }

    @ParameterizedTest
    @MethodSource("depositInvalidData")
    public void userCanNotDepositOnHisAccountWithInvalidData(Object balance, int statusCode) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' }.balance");

        //блок IF для преведения числа 100 из 3го кейса к строке.
        //без него строковое значение собиралось как число
        String balanceValue;

        if (balance instanceof String) {
            balanceValue = "\"" + balance + "\"";
        } else {
            balanceValue = String.valueOf(balance);
        }

        String requestBody = String.format("""
                {
                    "id": 1,
                        "balance": %s
                }
                """, balanceValue);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(statusCode);

        Float balanceAfterDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' }.balance");

        assertEquals(balanceAfterDeposit, initialBalance, 0.01);
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    public void userCanNotDepositOnInvalidAccount(int accountId) {

        String requestBody = String.format("""
                {
                    "id": %s,
                    "balance": 100
                }
                """, accountId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }


}
