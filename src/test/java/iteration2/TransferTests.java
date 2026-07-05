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
                Arguments.of(9_999),
                Arguments.of(10_000));
    }

    public static Stream<Arguments> invalidAmount() {
        return Stream.of(
                Arguments.of(10_001, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-1, HttpStatus.SC_BAD_REQUEST),
                Arguments.of("500", HttpStatus.SC_INTERNAL_SERVER_ERROR),
                Arguments.of(null, HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferBetweenOwnAccounts(int amount) {
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
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(Object amount, int statusCode) {
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
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);
    }


    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(Object amount) {
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
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(Object amount, int statusCode) {
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
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);
    }

    @Test
    public void userCanNotTransferWhenAmountMoreThanBalance() {
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
                .header("Authorization", "Basic Tmlrb2xheTpOaWtvbGF5MTIzJFBhc3N3b3JkJA==")
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

}
