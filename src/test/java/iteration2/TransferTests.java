package iteration2;

import generators.RandomData;
import io.restassured.http.ContentType;
import models.UserRole;
import models.requests.CreateUserRequest;
import models.requests.DepositMoneyRequest;
import models.requests.TransferMoneyRequest;
import models.responses.CreateAccountResponse;
import models.responses.GetUserAccountsResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTests {

    public static Stream<Arguments> validAmount() {
        return Stream.of(
                Arguments.of(new BigDecimal("5000"), new BigDecimal("9999.99")),
                Arguments.of(new BigDecimal("5000"), new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("5000"), new BigDecimal("10000"))
        );
    }

    public static Stream<Arguments> invalidAmount() {
        return Stream.of(
                Arguments.of(new BigDecimal("5000"), new BigDecimal("10000.01"), "Transfer amount cannot exceed 10000"),
                Arguments.of(new BigDecimal("5000"), new BigDecimal("0"), "Transfer amount must be at least 0.01"),
                Arguments.of(new BigDecimal("5000"), new BigDecimal("-0.01"), "Transfer amount must be at least 0.01")
        );
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferBetweenOwnAccounts(BigDecimal depositAmount, BigDecimal transferAmount) {
        //готовим данные для создания пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        //создаем пользователя
        String userAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");

        //создаем 1 счет
        var firstAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //создаем 2 счет
        var secondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //вытаскиваем айдишки счетов и баланс 2го счета
        long firstAccountId = firstAccountResponse.getId();
        long secondAccountId = secondAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondAccountResponse.getBalance();

        //готовим данные для пополнения счета
        var depositMoneyUserRequest = DepositMoneyRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();

        //депозитим для будущих трансферов (3 депозита)
        for (int i = 0; i < 3; i++) {
            new DepositMoneyRequester(
                    RequestSpecs.authAsUser(userAuthToken),
                    ResponseSpecs.requestReturnsOK()
            )
                    .post(depositMoneyUserRequest);
        }

        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс со второго счета
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        BigDecimal expectedBalance = secondAccountInitialBalance.add(transferAmount);

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(BigDecimal depositAmount, BigDecimal transferAmount, String errorValue) {
        //готовим данные для создания пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        //создаем пользователя
        String userAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");

        //создаем 1 счет
        var firstAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //создаем 2 счет
        var secondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //вытаскиваем айдишки счетов и баланс 2го счета
        long firstAccountId = firstAccountResponse.getId();
        long secondAccountId = secondAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondAccountResponse.getBalance();

        //готовим данные для пополнения счета
        var depositMoneyUserRequest = DepositMoneyRequest.builder()
                .id(firstAccountId)
                .balance(depositAmount)
                .build();

        //депозитим для будущих трансферов (3 депозита)
        for (int i = 0; i < 3; i++) {
            new DepositMoneyRequester(
                    RequestSpecs.authAsUser(userAuthToken),
                    ResponseSpecs.requestReturnsOK()
            )
                    .post(depositMoneyUserRequest);
        }

        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс со второго счета
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        BigDecimal expectedBalance = secondAccountInitialBalance;

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }


    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(double amount) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String firstUserName = UUID.randomUUID().toString().substring(0, 8);
        String secondUserName = UUID.randomUUID().toString().substring(0, 8);

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
                """, firstUserAccountId);

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
                """, firstUserAccountId, secondUserAccountId, amount);

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
        String firstUserName = UUID.randomUUID().toString().substring(0, 8);
        String secondUserName = UUID.randomUUID().toString().substring(0, 8);

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
                """, firstUserAccountId);

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
                """, firstUserAccountId, secondUserAccountId, amountValue);

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
        String userName = UUID.randomUUID().toString().substring(0, 8);

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
