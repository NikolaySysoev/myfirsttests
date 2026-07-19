package iteration2;

import models.requests.DepositMoneyRequest;
import models.responses.DepositMoneyResponse;
import models.responses.GetUserAccountsResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DepositTest {

    private String userAuthToken;
    private BigDecimal userInitialBalance;
    private long userAccountId;
    private String username;
    private String password;

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
        //депозит
        var request = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).
                post(request);

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
    public void userCanNotDepositOnInvalidAccount(int accountId, String errorValue) {
        //создаем объект запроса на депозит
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(new BigDecimal(RandomStringUtils.randomNumeric(1,3)))
                .build();

        //делаем пост запрос на депозит
        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.DEPOSIT_MONEY,
                ResponseSpecs.requestReturnsForbidden(errorValue)
        )
                .post(depositMoneyRequest);
    }
}
