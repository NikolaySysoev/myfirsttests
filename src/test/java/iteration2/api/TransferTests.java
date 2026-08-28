package iteration2.api;

import api.dao.checks.DbChecks;
import api.generators.RandomData;
import api.models.domain.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.v1.requests.TransferMoneyRequest;
import api.models.v1.responses.TransferMoneyResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static iteration2.TestUtils.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTests extends BaseApiTest {
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");

    private long senderAccountId;
    private long receiverAccountId;
    private BigDecimal senderAccountBalanceAfterSetup;
    private BigDecimal receiverAccountBalanceAfterSetup;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private static final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());

    @BeforeEach
    public void setup() {
        // пользователь уже создан ApiUserSessionExtension'ом (по @UserSession на тестовом методе)

        //создание 1го аккаунта (sender)
        var firstAccountResponse = SessionStorage.actAsUser().createAccount();

        //создание 2го аккаунта (receiver)
        var secondAccountResponse = SessionStorage.actAsUser().createAccount();

        //вытаскиваем айдишки счетов
        senderAccountId = firstAccountResponse.getId();
        receiverAccountId = secondAccountResponse.getId();

        //вытаскиваем номера аккаунтов
        senderAccountNumber = firstAccountResponse.getAccountNumber();
        receiverAccountNumber = secondAccountResponse.getAccountNumber();

        //депозитим для будущих трансферов (3 депозита)
        repeat(3, () -> SessionStorage.actAsUser().depositMoney(senderAccountId, DEFAULT_DEPOSIT));

        //вытаскиваем баланс с первого счета
        senderAccountBalanceAfterSetup = SessionStorage.actAsUser().getAccountBalance(senderAccountId);
        //вытаскиваем баланс со второго счета
        receiverAccountBalanceAfterSetup = SessionStorage.actAsUser().getAccountBalance(receiverAccountId);
    }

    public static Stream<Arguments> validAmount() {
        return Stream.of(
                Arguments.of(new BigDecimal("9999.99")),
                Arguments.of(new BigDecimal("0.01")),
                Arguments.of(new BigDecimal("10000"))
        );
    }

    public static Stream<Arguments> invalidAmount() {
        return Stream.of(
                Arguments.of(new BigDecimal("10000.01"), ApiError.TRANSFER_HIGHER_BOUNDARY),
                Arguments.of(new BigDecimal("0"), ApiError.TRANSFER_LOWER_BOUNDARY),
                Arguments.of(new BigDecimal("-0.01"), ApiError.TRANSFER_LOWER_BOUNDARY)
        );
    }

    public static Stream<Arguments> insufficientFundsData() {
        return Stream.of(
                Arguments.of(randomBalance, ApiError.TRANSFER_INSUFFICIENT_FUNDS)
        );
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferBetweenOwnAccounts(BigDecimal transferAmount, DbChecks db) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        var transferMoneyResponse = new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();

        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser().getAccountByAccountNumber(receiverAccountNumber);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = senderAccount.getBalance();
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = receiverAccount.getBalance();

        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup.subtract(transferAmount);
        //ожидаем что на 2 счете теперь баланс стал больше на сумму трансфера
        BigDecimal receiverAccountExpectedBalance = receiverAccountBalanceAfterSetup.add(transferAmount);

        //проверяем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, receiverAccountExpectedBalance.compareTo(receiverAccountBalanceAfterTransfer));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));

        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverAccountNumber));
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(BigDecimal transferAmount, ApiError errorValue, DbChecks db) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser().getAccountByAccountNumber(receiverAccountNumber);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = senderAccount.getBalance();
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = receiverAccount.getBalance();

        //ожидаем что баланс 1 и 2 счета не изменились
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup;
        BigDecimal receiverAccountExpectedBalance = receiverAccountBalanceAfterSetup;

        //првоеряем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, receiverAccountExpectedBalance.compareTo(receiverAccountBalanceAfterTransfer));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));

        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverAccountNumber));
    }

    @UserSession(2)
    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(BigDecimal transferAmount, DbChecks db) {
        //создаем счет второму пользователю (он уже создан и залогинен ApiUserSessionExtension'ом)
        var secondUserAccountResponse = SessionStorage.actAsUser(2).createAccount();

        String receiverUserAccountNumber= secondUserAccountResponse.getAccountNumber();
        long receiverUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //запрос на трансфер
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverUserAccountId)
                .amount(transferAmount)
                .build();

        var transferMoneyResponse = new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();

        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser(2).getAccountByAccountNumber(receiverUserAccountNumber);

        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = senderAccount.getBalance();
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondAccountBalanceAfterTransfer = receiverAccount.getBalance();

        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup.subtract(transferAmount);
        //ожидаем что на счете 2го пользователя теперь баланс стал больше на сумму трансфера
        BigDecimal secondUserExpectedBalance = secondAccountInitialBalance.add(transferAmount);

        //проверяем баланс счета 1го пользователя
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс счета 2го пользователя
        assertEquals(0, secondUserExpectedBalance.compareTo(secondAccountBalanceAfterTransfer));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));

        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverUserAccountNumber));
    }

    @UserSession(2)
    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(BigDecimal transferAmount, ApiError errorValue, DbChecks db) {
        //создаем счет второму пользователю
        var secondUserAccountResponse = SessionStorage.actAsUser(2).createAccount();

        String receiverUserAccountNumber= secondUserAccountResponse.getAccountNumber();
        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //трансфер денег
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser(2).getAccountByAccountNumber(receiverUserAccountNumber);

        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = senderAccount.getBalance();
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondUserAccountBalanceAfterTransfer = receiverAccount.getBalance();

        //ожидаем что баланс счета 1 пользователя не изменился
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup;
        //ожидаем что баланс счета 2 пользователя не изменился
        BigDecimal secondUserExpectedBalance = secondAccountInitialBalance;

        //првоеряем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, secondUserExpectedBalance.compareTo(secondUserAccountBalanceAfterTransfer));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));

        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverUserAccountNumber));
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("insufficientFundsData")
    public void userCanNotTransferWhenAmountMoreThanBalance(BigDecimal transferAmount, ApiError errorValue, DbChecks db) {
        //готовим данные для трансфера
        //счета поменяны местами, чтобы с нулевого переводить на счет с деньгами
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(receiverAccountId)
                .receiverAccountId(senderAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //счета поменяны местами намеренно, чтобы с нулевого переводить на счет с деньгами
        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser().getAccountByAccountNumber(receiverAccountNumber);

        //вытаскиваем баланс со второго счета
        BigDecimal receiverBalanceAfterTransfer = receiverAccount.getBalance();
        BigDecimal expectedBalance = receiverAccountBalanceAfterSetup;

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(receiverBalanceAfterTransfer));

        //Проверка в БД. Сравнивается Гет юзер аккаунт и запись в БД по аккаунт номеру
        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverAccountNumber));

        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));
    }
}
