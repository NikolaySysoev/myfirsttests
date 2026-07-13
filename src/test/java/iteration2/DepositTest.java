package iteration2;

import generators.RandomData;
import io.restassured.http.ContentType;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.UserCreateAccountRequester;
import requests.UserDepositMoneyRequester;
import requests.UserGetAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositTest {

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(new BigDecimal("4999.99")),
                Arguments.of(new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("5000.00")));
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
                Arguments.of(1, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999999, HttpStatus.SC_BAD_REQUEST));
    }

    @ParameterizedTest
    @MethodSource("depositValidData")
    public void userCanDepositOnHisAccount(BigDecimal balance) {
        //создание
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        //создание нового пользователя с админской учетки и сохранение его токена
        String userAuthToken = new AdminCreateUserRequester(
                //админ реквест спека - хэддеры запроса + фильтры(для консоли) + бейс uri
                RequestSpecs.adminSpec(),
                //респонс спека - проверяет что статус ответа 201
                ResponseSpecs.entityWasCreated()
        )
                //пост запрос вызывается у класса AdminCreateUserRequest
                //берем объект созданный выше и прокидывает его в запрос
                //объект тут = model в аргументе запроса в методе класса. Тут юзернейм, пароль и роль.
                .post(createUserRequest)
                .extract()
                .header("authorization");

        //создание аккаунта от лица пользователя, которого создали на прошлом шаге
        CreateAccountUserResponse accountResponse = new UserCreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountUserResponse.class);

        Long accountId = accountResponse.getId();

        //стартовый баланс пользователя
        BigDecimal initialBalance = new BigDecimal("0.00");

        //создаем объект запроса на депозит
        DepositMoneyUserRequest depositMoneyUserRequest = DepositMoneyUserRequest.builder()
                .id(accountId)
                .balance(balance)
                .build();

        //делаем пост запрос на депозит
        new UserDepositMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .post(depositMoneyUserRequest);

        //делаем гет запрос на проверку изменения данных
        GetUserAccountsResponse[] accounts = new UserGetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        BigDecimal balanceAfterDeposit = accounts[0].getBalance();
        BigDecimal expectedBalance = initialBalance.add(balance);

        //сравниваем 0 и результат сравнения двух переменных - ожидаемый баланс и баланс после депозита.
        // если ожидаемый и после депозита равны -> компаратор вернет 0
        // если ожидаемый < депозита -> компаратор вернет отр. число
        // если ожидаемый > депозита -> компаратор вернет положит. число
        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @ParameterizedTest
    @MethodSource("depositInvalidData")
    public void userCanNotDepositOnHisAccountWithInvalidData(Object balance, int statusCode) {
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
        Integer userAccountId = given()
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
                    "id": %s,
                        "balance": %s
                }
                """,userAccountId, balanceValue);

        //пост запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(statusCode);

        //проверка баланса
        Float balanceAfterDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("[0].balance");

        assertEquals(initialBalance, balanceAfterDeposit, 0.01);
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    public void userCanNotDepositOnInvalidAccount(int accountId) {
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

        String requestBody = String.format("""
                {
                    "id": %s,
                    "balance": 100
                }
                """, accountId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
