package iteration2.api;

import api.generators.RandomData;
import api.models.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.requests.DepositMoneyRequest;
import api.models.responses.DepositMoneyResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.TestType;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DepositTest extends BaseApiTest {

    private BigDecimal userInitialBalance;
    private long userAccountId;
    private static final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());

    @BeforeEach
    public void setup() {
        // пользователь уже создан ApiUserSessionExtension'ом (по @UserSession на тестовом методе)
        var createAccountResponse = SessionStorage.actAsUser().createAccount();

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


    @UserSession
    @ParameterizedTest
    @MethodSource("depositValidData")
    @DisplayName("Юзер может пополнить акк")
    @TestType("regress")
    public void userCanDepositOnHisAccount(BigDecimal balance) {
        //депозит
        var request = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        var response = new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).
                post(request);

        //echo проверка id аккаунта
        ModelAssertions.assertThatModels(request, response).match();

        //баланс после депозита через шаги пользователя из хранилища
        BigDecimal balanceAfterDeposit = SessionStorage.actAsUser().getAccountBalance(userAccountId);
        BigDecimal expectedBalance = userInitialBalance.add(balance);

        //сравниваем 0 и результат сравнения двух переменных - ожидаемый баланс и баланс после депозита.
        // если ожидаемый и после депозита равны -> компаратор вернет 0
        // если ожидаемый < депозита -> компаратор вернет отр. число
        // если ожидаемый > депозита -> компаратор вернет положит. число
        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("depositInvalidData")
    @DisplayName("Юзер не может пополнить при невалидных данных")
    @TestType({"regress", "smoke"})
    public void userCanNotDepositOnHisAccountWithInvalidData(BigDecimal balance, String errorValue) {
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(userAccountId)
                .balance(balance)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(depositMoneyRequest);

        BigDecimal expectedBalance = userInitialBalance;
        BigDecimal balanceAfterDeposit = SessionStorage.actAsUser().getAccountBalance(userAccountId);

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }

    @UserSession(2)
    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    @DisplayName("Юзер не может пополнить чужой/не сущ. аккаунт")
    @TestType("smoke")
    public void userCanNotDepositOnInvalidAccount(String errorValue) {

        var secondUserAccountId = SessionStorage.actAsUser(2).createAccount().getId();

        //создаем объект запроса на депозит
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(secondUserAccountId)
                .balance(randomBalance)
                .build();

        //делаем пост запрос на депозит от лица первого пользователя на счет второго
        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.DEPOSIT_MONEY,
                ResponseSpecs.requestReturnsForbidden(errorValue)
        )
                .post(depositMoneyRequest);

        //проверяем акк 2го пользователя, убеждаемся что баланс не изменился
        BigDecimal expectedBalance = new BigDecimal("0.00");
        BigDecimal balanceAfterDeposit = SessionStorage.actAsUser(2).getAccountBalance(secondUserAccountId);

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));
    }
}
