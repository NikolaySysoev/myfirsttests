package iteration2;

import models.ApiError;
import models.assertions.ModelAssertions;
import models.requests.DepositMoneyRequest;
import models.responses.DepositMoneyResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static iteration2.TestUtils.getAccountBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class DepositTest {

    private BigDecimal userInitialBalance;
    private long userAccountId;
    private String username;
    private String password;
    BigDecimal randomBalance = new BigDecimal(RandomStringUtils.randomNumeric(1,3));

    @BeforeEach
    public void setup() {
        //создаем пользователя
        var createUserRequest = AdminSteps.createUser();

        //создание 1го аккаунта (sender)
        var createAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        username = createUserRequest.getUsername();
        password = createUserRequest.getPassword();

        //вытаскиваем айдишку счета и стартовый баланс
        userAccountId = createAccountResponse.getId();
        userInitialBalance = createAccountResponse.getBalance();
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
                Arguments.of(new BigDecimal("0.00"), ApiError.DEPOSIT_LOWER_BOUNDARY.getMessage()),
                Arguments.of(new BigDecimal("5000.01"), ApiError.DEPOSIT_HIGHER_BOUNDARY.getMessage()),
                Arguments.of(new BigDecimal("-0.01"), ApiError.DEPOSIT_LOWER_BOUNDARY.getMessage())
        );
    }

    public static Stream<Arguments> depositInvalidAccount() {
        return Stream.of(
                Arguments.of(ApiError.DEPOSIT_FORBIDDEN.getMessage())
        );
    }

    @ParameterizedTest
    @MethodSource("depositValidData")
    @DisplayName("Юзер может пополнить акк")
    public void userCanDepositOnHisAccount(BigDecimal balance) {
        //депозит
        var request = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        var response = new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).
                post(request);

        //echo проверка id аккаунта
        ModelAssertions.assertThatModels(request,response).match();

        //делаем гет запрос на проверку изменения данных
        var accounts = UserSteps.getAccounts(username, password);

        BigDecimal balanceAfterDeposit = UserSteps.getAccountBalance(accounts, userAccountId);
        BigDecimal expectedBalance = userInitialBalance.add(balance);

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
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(depositMoneyRequest);

        var accounts = UserSteps.getAccounts(username, password);


        BigDecimal expectedBalance = userInitialBalance;
        BigDecimal balanceAfterDeposit = UserSteps.getAccountBalance(accounts, userAccountId);

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    @DisplayName("Юзер не может пополнить чужой/не сущ. аккаунт")
    public void userCanNotDepositOnInvalidAccount(String errorValue) {

        var userRequest = AdminSteps.createUser();
        var secondUserAccountId = UserSteps.createAccount(userRequest.getUsername(), userRequest.getPassword()).getId();

        //создаем объект запроса на депозит
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(secondUserAccountId)
                .balance(randomBalance)
                .build();

        //делаем пост запрос на депозит
        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.DEPOSIT_MONEY,
                ResponseSpecs.requestReturnsForbidden(errorValue)
        )
                .post(depositMoneyRequest);

        //проверяем акк 2го пользователя, убеждаемся что баланс не изменился
        var accounts = UserSteps.getAccounts(userRequest.getUsername(), userRequest.getPassword());

        BigDecimal expectedBalance = new BigDecimal("0.00");
        BigDecimal balanceAfterDeposit = getAccountBalance(accounts, secondUserAccountId);

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));

    }
}
