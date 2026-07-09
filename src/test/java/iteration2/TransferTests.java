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
import java.util.UUID;
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
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица пользователя и вытаскиваем его айди
        Integer userFirstAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        //создаем тело для пост запроса на пополнение баланса
        String depositRequestBody = String.format("""
                {
                    "id": %s,
                        "balance": 5000
                }
                """,userFirstAccountId);
        //делаем запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //создаем второй счет от лица пользователя и вытаскиваем его айди
        Integer userSecondAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float initialBalanceOfSecondAccount = 0.00f;


        String requestBody = String.format("""
                {
                  "senderAccountId": %s,
                  "receiverAccountId": %s,
                  "amount": %s
                }
                """,userFirstAccountId, userSecondAccountId, amount);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path(String.format("find { it.id == %d }.balance", userSecondAccountId));

        assertEquals(initialBalanceOfSecondAccount + amount, balanceAfterTransfer, 0.01);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(Object amount, int statusCode) {
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
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица пользователя и вытаскиваем его айди
        Integer userFirstAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        //создаем тело для пост запроса на пополнение баланса
        String depositRequestBody = String.format("""
                {
                    "id": %s,
                        "balance": 5000
                }
                """,userFirstAccountId);

        //делаем запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //создаем второй счет от лица пользователя и вытаскиваем его айди
        Integer userSecondAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float initialBalanceOfSecondAccount = 0.00f;

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
                  "senderAccountId": %s,
                  "receiverAccountId": %s,
                  "amount": %s
                }
                """,userFirstAccountId, userSecondAccountId, amountValue);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path(String.format("find { it.id == %d }.balance", userSecondAccountId));

        assertEquals(initialBalanceOfSecondAccount, balanceAfterTransfer, 0.01);
    }


    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(double amount) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String firstUserName = UUID.randomUUID().toString().substring(0,8);
        String secondUserName = UUID.randomUUID().toString().substring(0,8);

        String createFirstUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, firstUserName);

        String createSecondUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, secondUserName);

        //создаем 1-го пользователя и тащим токен
        String firstUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createFirstUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица 1-го пользователя и вытаскиваем его айди
        Integer firstUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        //создаем тело для пост запроса на пополнение баланса
        String depositRequestBody = String.format("""
                {
                    "id": %s,
                        "balance": 5000
                }
                """,firstUserAccountId);

        //делаем запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //создаем 2-го пользователя и тащим токен
        String secondUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createSecondUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица 2-го пользователя и вытаскиваем его айди
        Integer secondUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", secondUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float secondUserInitialBalance = 0.00f;

        String requestBody = String.format("""
                {
                  "senderAccountId": %s,
                  "receiverAccountId": %s,
                  "amount": %s
                }
                """,firstUserAccountId, secondUserAccountId, amount);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", secondUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path(String.format("find { it.id = %s }.balance", secondUserAccountId));

        assertEquals(secondUserInitialBalance + amount, balanceAfterTransfer, 0.01);
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(Object amount, int statusCode) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String firstUserName = UUID.randomUUID().toString().substring(0,8);
        String secondUserName = UUID.randomUUID().toString().substring(0,8);

        String createFirstUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, firstUserName);

        String createSecondUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, secondUserName);

        //создаем 1-го пользователя и тащим токен
        String firstUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createFirstUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица 1-го пользователя и вытаскиваем его айди
        Integer firstUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        //создаем тело для пост запроса на пополнение баланса
        String depositRequestBody = String.format("""
                {
                    "id": %s,
                        "balance": 5000
                }
                """,firstUserAccountId);

        //делаем запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(depositRequestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //создаем 2-го пользователя и тащим токен
        String secondUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createSecondUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица 2-го пользователя и вытаскиваем его айди
        Integer secondUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", secondUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float secondUserInitialBalance = 0.00f;

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
                  "senderAccountId": %s,
                  "receiverAccountId": %s,
                  "amount": %s
                }
                """,firstUserAccountId, secondUserAccountId, amountValue);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", firstUserAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(statusCode);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", secondUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path(String.format("find { it.id = %s }.balance", secondUserAccountId));

        assertEquals(secondUserInitialBalance, balanceAfterTransfer, 0.01);
    }

    @Test
    public void userCanNotTransferWhenAmountMoreThanBalance() {
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
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица пользователя и вытаскиваем его айди
        Integer userFirstAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        //создаем второй счет от лица пользователя и вытаскиваем его айди
        Integer userSecondAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float initialBalance = 0.00f;

        String requestBody = String.format("""
                {
                  "senderAccountId": %s,
                  "receiverAccountId": %s,
                  "amount": 100
                }
                """, userFirstAccountId, userSecondAccountId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        Float balanceAfterTransfer = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path(String.format("find { it.id == %d }.balance", userSecondAccountId));

        assertEquals(initialBalance, balanceAfterTransfer);
    }

}
