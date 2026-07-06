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
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTests {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> validAmount() {
        return Stream.of(
                Arguments.of(9_999.99),
                Arguments.of(0.01),
                Arguments.of(10_000));
    }

    public static Stream<Arguments> invalidAmount() {
        return Stream.of(
                Arguments.of(10_000.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-0.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of("500", HttpStatus.SC_INTERNAL_SERVER_ERROR),
                Arguments.of(null, HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferBetweenOwnAccounts(double amount) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC2' }.balance");

        String requestBody = String.format("""
                {
                  "senderAccountId": 1,
                  "receiverAccountId": 2,
                  "amount": %s
                }
                """, amount);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC2' }.balance");

        assertEquals(initialBalance + amount, balanceAfterTransfer, 0.01);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(Object amount, int statusCode) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC2' }.balance");

        //блок IF для преведения числа 100 из 3го кейса к строке.
        //без него строковое значение собиралось как число
        String amountValue;

        if (amount instanceof String) {
            amountValue = "\"" + amount + "\"";
        } else {
            amountValue = String.valueOf(amount);
        }

        String requestBody = String.format("""
                {
                  "senderAccountId": 1,
                  "receiverAccountId": 2,
                  "amount": %s
                }
                """, amountValue);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC2' }.balance");

        assertEquals(initialBalance, balanceAfterTransfer, 0.01);
    }


    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(double amount) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic TWFyeTpNYXJ5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC3' }.balance");

        String requestBody = String.format("""
                {
                  "senderAccountId": 1,
                  "receiverAccountId": 3,
                  "amount": %s
                }
                """, amount);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic TWFyeTpNYXJ5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC3' }.balance");

        assertEquals(initialBalance + amount, balanceAfterTransfer, 0.01);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(Object amount, int statusCode) {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic TWFyeTpNYXJ5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC3' }.balance");

        //блок IF для преведения числа 100 из 3го кейса к строке.
        //без него строковое значение собиралось как число
        String amountValue;

        if (amount instanceof String) {
            amountValue = "\"" + amount + "\"";
        } else {
            amountValue = String.valueOf(amount);
        }

        String requestBody = String.format("""
                {
                  "senderAccountId": 1,
                  "receiverAccountId": 3,
                  "amount": %s
                }
                """, amountValue);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic TWFyeTpNYXJ5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC3' }.balance");

        assertEquals(initialBalance, balanceAfterTransfer, 0.01);
    }

    @Test
    public void userCanNotTransferWhenAmountMoreThanBalance() {
        Float initialBalance = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' } .balance");

        //на 4м аккаунте баланс = 0
        String requestBody = """
                {
                  "senderAccountId": 4,
                  "receiverAccountId": 1,
                  "amount": 100
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3Jk")
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("find { it.accountNumber == 'ACC1' }.balance");

        assertEquals(initialBalance, balanceAfterTransfer, 0.01);
    }

}
