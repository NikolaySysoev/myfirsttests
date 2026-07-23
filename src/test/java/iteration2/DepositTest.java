package iteration2;

import generators.RandomData;
import models.*;
import models.requests.CreateUserRequest;
import models.requests.DepositMoneyRequest;
import models.responses.CreateAccountResponse;
import models.responses.GetUserAccountsResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import requests.GetAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.stream.Stream;


public class DepositTest {

    BigDecimal randomBalance = new BigDecimal(RandomStringUtils.randomNumeric(1,3));

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(new BigDecimal("4999.99")),
                Arguments.of(new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("5000.00"))
        );
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(new BigDecimal("0.00"), "Deposit amount must be at least 0.01"),
                Arguments.of(new BigDecimal("5000.01"), "Deposit amount cannot exceed 5000"),
                Arguments.of(new BigDecimal("-0.01"), "Deposit amount must be at least 0.01")
        );
    }

    public static Stream<Arguments> depositInvalidAccount() {
        return Stream.of(
                Arguments.of(1, "Unauthorized access to account"),
                Arguments.of(999999999, "Unauthorized access to account")
        );
    }

    @ParameterizedTest
    @MethodSource("depositValidData")
    @DisplayName("Юзер может пополнить акк")
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
        CreateAccountResponse accountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post()
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        long accountId = accountResponse.getId();

        //стартовый баланс пользователя
        BigDecimal initialBalance = new BigDecimal("0.00");

        //создаем объект запроса на депозит
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(balance)
                .build();

        //делаем пост запрос на депозит
        new DepositMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .post(depositMoneyRequest);

        //делаем гет запрос на проверку изменения данных
        GetUserAccountsResponse[] accounts = new GetAccountsRequester(
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
    @DisplayName("Юзер не может пополнить при невалидных данных")
    public void userCanNotDepositOnHisAccountWithInvalidData(BigDecimal balance, String errorValue) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        String userAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");

        CreateAccountResponse accountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post()
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        long accountId = accountResponse.getId();

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(balance)
                .build();

        new DepositMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(depositMoneyRequest);

        GetUserAccountsResponse[] accounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        BigDecimal expectedBalance = new BigDecimal("0.00");
        BigDecimal balanceAfterDeposit = accounts[0].getBalance();

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    @DisplayName("Юзер не может пополнить чужой/не сущ. аккаунт")
    public void userCanNotDepositOnInvalidAccount(int accountId, String errorValue) {
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


        //создаем объект запроса на депозит
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(randomBalance)
                .build();

        //делаем пост запрос на депозит
        new DepositMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsForbidden(errorValue)
        )
                .post(depositMoneyRequest);
    }
}
