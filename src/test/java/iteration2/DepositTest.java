package iteration2;

import generators.RandomEntityGenerator;
import models.requests.CreateUserRequest;
import models.requests.DepositMoneyRequest;
import models.requests.LoginRequest;
import models.responses.CreateAccountResponse;
import models.responses.CreateUserResponse;
import models.responses.DepositMoneyResponse;
import models.responses.GetUserAccountsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DepositTest {
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("0.00");

    private String userAuthToken;
    private long userAccountId;


    //хэлпер для получения баланса пользователя
    private BigDecimal getAccountBalance(GetUserAccountsResponse[] accounts, long accountId) {
        return Arrays.stream(accounts)
                .filter(acc -> acc.getId() == accountId)
                .map(GetUserAccountsResponse::getBalance)
                .findFirst()
                .orElseThrow();
    }

    @BeforeEach
    public void setup() {
        //готовим данные для создания пользователя
        var createUserRequest = RandomEntityGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_CREATE_USERS,
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest);

        //логин под созданным пользователем
        var loginRequest = LoginRequest.builder()
                .username(createUserRequest.getUsername())
                .password(createUserRequest.getPassword())
                .build();

        userAuthToken = new CrudRequester(
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginRequest)
                .extract()
                .header("authorization");

        //создание 1го аккаунта (sender)
        var createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);


        //вытаскиваем айдишку счета
        userAccountId = createAccountResponse.getId();
    }

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
        //стартовый баланс пользователя
        BigDecimal initialBalance = DEFAULT_BALANCE;

        //создаем объект запроса на депозит
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        //делаем пост запрос на депозит
        new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        )
                .post(depositMoneyRequest);

        //делаем гет запрос на проверку изменения данных
        var accounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserAccountsResponse[].class);

        BigDecimal balanceAfterDeposit = getAccountBalance(accounts, userAccountId);
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
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(depositMoneyRequest);

        var accounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        BigDecimal expectedBalance = DEFAULT_BALANCE;
        BigDecimal balanceAfterDeposit = getAccountBalance(accounts, userAccountId);

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    @DisplayName("Юзер не может пополнить чужой/не сущ. аккаунт")
    public void userCanNotDepositOnInvalidAccount(int accountId, String errorValue) {
        //создаем объект запроса на депозит
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(new BigDecimal("100"))
                .build();

        //делаем пост запрос на депозит
        new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.DEPOSIT_MONEY,
                ResponseSpecs.requestReturnsForbidden(errorValue)
        )
                .post(depositMoneyRequest);
    }
}
