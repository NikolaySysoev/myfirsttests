package iteration2.api;

import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomData;
import api.models.domain.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.factory.DtoFactory;
import api.models.BaseModel;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
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

/**
 * Тест не привязан к версии бэкенда: DTO запроса собирает {@link DtoFactory},
 * которую подставляет ApiVersionExtension по активной версии.
 * <p>
 * Аннотации @ApiVersion нет — значит тест идёт на версию по умолчанию (V2)
 * либо на ту, что задана через -DbackendVersion. Чтобы принудительно оставить
 * тест на легаси, достаточно повесить @ApiVersion(BackendVersion.V1)
 * на метод или на класс.
 */
public class DepositTest extends BaseApiTest {

    private BigDecimal userInitialBalance;
    private long userAccountId;
    private String userAccountNumber;
    private static final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());

    @BeforeEach
    public void setup() {
        // пользователь уже создан ApiUserSessionExtension'ом (по @UserSession на тестовом методе)
        var createAccountResponse = SessionStorage.actAsUser().createAccount();

        //вытаскиваем айдишку счета и стартовый баланс
        userAccountId = createAccountResponse.getId();
        userInitialBalance = createAccountResponse.getBalance();
        userAccountNumber = createAccountResponse.getAccountNumber();
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(new BigDecimal("4999.99")),
                Arguments.of(new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("5000.00"))
        );
    }

    /**
     * В Arguments кладём саму константу ApiError, а не её текст.
     * <p>
     * @MethodSource вычисляется на этапе построения инвокаций параметризованного
     * теста — ДО beforeEach, то есть до того, как ApiVersionExtension установил
     * версию. Если звать getMessage() здесь, текст ошибки отрезолвится на пустом
     * контексте. В теле теста версия уже определена, поэтому текст берём там.
     */
    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(new BigDecimal("0.00"), ApiError.DEPOSIT_LOWER_BOUNDARY),
                Arguments.of(new BigDecimal("5000.01"), ApiError.DEPOSIT_HIGHER_BOUNDARY),
                Arguments.of(new BigDecimal("-0.01"), ApiError.DEPOSIT_LOWER_BOUNDARY)
        );
    }

    public static Stream<Arguments> depositInvalidAccount() {
        return Stream.of(
                Arguments.of(ApiError.DEPOSIT_FORBIDDEN)
        );
    }


    @UserSession
    @ParameterizedTest
    @MethodSource("depositValidData")
    @DisplayName("Юзер может пополнить акк")
    public void userCanDepositOnHisAccount(BigDecimal amount, DtoFactory dto) {
        //депозит: DTO собирается под активную версию контракта
        var request = dto.deposit(userAccountId, amount);

        var response = new ValidatedCrudRequester<BaseModel>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).
                post(request);

        //echo проверка id аккаунта
        ModelAssertions.assertThatModels(request, response).match();

        //баланс после депозита через шаги пользователя из хранилища
        var customerProfile = SessionStorage.actAsUser().getAccounts();
        BigDecimal balanceAfterDeposit = customerProfile.getFirst().getBalance();
        BigDecimal expectedBalance = userInitialBalance.add(amount);

        //сравниваем 0 и результат сравнения двух переменных - ожидаемый баланс и баланс после депозита.
        // если ожидаемый и после депозита равны -> компаратор вернет 0
        // если ожидаемый < депозита -> компаратор вернет отр. число
        // если ожидаемый > депозита -> компаратор вернет положит. число
        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        var userAccountDao = DataBaseSteps.getAccountByAccountNumber(userAccountNumber);
        DaoAndModelAssertions.assertThat(customerProfile.getFirst(), userAccountDao).match();
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("depositInvalidData")
    @DisplayName("Юзер не может пополнить при невалидных данных")
    public void userCanNotDepositOnHisAccountWithInvalidData(BigDecimal amount, ApiError error, DtoFactory dto) {
        var depositMoneyRequest = dto.deposit(userAccountId, amount);

        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequest(error)
        )
                .post(depositMoneyRequest);

        BigDecimal expectedBalance = userInitialBalance;

        var customerProfile = SessionStorage.actAsUser().getAccounts();
        BigDecimal balanceAfterDeposit = customerProfile.getFirst().getBalance();

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        var userAccountDao = DataBaseSteps.getAccountByAccountNumber(userAccountNumber);
        DaoAndModelAssertions.assertThat(customerProfile.getFirst(), userAccountDao).match();
    }

    @UserSession(2)
    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    @DisplayName("Юзер не может пополнить чужой/не сущ. аккаунт")
    public void userCanNotDepositOnInvalidAccount(ApiError error, DtoFactory dto) {

        var secondUserAccount = SessionStorage.actAsUser(2).createAccount();
        var secondUserAccountId = secondUserAccount.getId();
        var secondUserAccountNumber = secondUserAccount.getAccountNumber();

        //создаем объект запроса на депозит под активную версию контракта
        var depositMoneyRequest = dto.deposit(secondUserAccountId, randomBalance);

        //делаем пост запрос на депозит от лица первого пользователя на счет второго
        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(error)
        )
                .post(depositMoneyRequest);

        //проверяем акк 2го пользователя, убеждаемся что баланс не изменился
        BigDecimal expectedBalance = new BigDecimal("0.00");

        var secondCustomerAccount = SessionStorage.actAsUser(2).getAccounts();
        BigDecimal balanceAfterDeposit = secondCustomerAccount.getFirst().getBalance();

        assertEquals(0, expectedBalance.compareTo(balanceAfterDeposit));

        //Проверка в БД. Сравнивается Гет юзер аккаунт на втором аккаунте и запись в БД по аккаунт номеру второго юзера
        var userAccountDao = DataBaseSteps.getAccountByAccountNumber(secondUserAccountNumber);
        DaoAndModelAssertions.assertThat(secondCustomerAccount.getFirst(), userAccountDao).match();
    }
}
