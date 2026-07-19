package iteration2;

import models.requests.TransferMoneyRequest;
import models.responses.TransferMoneyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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

public class TransferTests extends BaseTest{
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");

    private long senderAccountId;
    private long receiverAccountId;
    private BigDecimal senderAccountBalanceAfterSetup;
    private BigDecimal receiverAccountBalanceAfterSetup;
    private String username;
    private String password;

    @BeforeEach
    public void setup() {
        //создаем пользователя
        var createUserRequest = AdminSteps.createUser();

        //создание 1го аккаунта (sender)
        var firstAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        //создание 2го аккаунта (receiver)
        var secondAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );
        username = createUserRequest.getUsername();
        password = createUserRequest.getPassword();

        //вытаскиваем айдишки счетов
        senderAccountId = firstAccountResponse.getId();
        receiverAccountId = secondAccountResponse.getId();

        //депозитим для будущих трансферов (3 депозита)
        for (int i = 0; i < 3; i++) {
            UserSteps.depositMoney(senderAccountId, DEFAULT_DEPOSIT, username, password);
        }

        var userAccounts = UserSteps.getAccounts(username, password);

        //вытаскиваем баланс с первого счета
        senderAccountBalanceAfterSetup = UserSteps.getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        receiverAccountBalanceAfterSetup = UserSteps.getAccountBalance(userAccounts, receiverAccountId);
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
                Arguments.of(new BigDecimal("10000.01"), "Transfer amount cannot exceed 10000"),
                Arguments.of(new BigDecimal("0"), "Transfer amount must be at least 0.01"),
                Arguments.of(new BigDecimal("-0.01"), "Transfer amount must be at least 0.01")
        );
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferBetweenOwnAccounts(BigDecimal transferAmount) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = UserSteps.getAccounts(username, password);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, receiverAccountId);

        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup.subtract(transferAmount);
        //ожидаем что на 2 счете теперь баланс стал больше на сумму трансфера
        BigDecimal receiverAccountExpectedBalance = receiverAccountBalanceAfterSetup.add(transferAmount);

        //проверяем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, receiverAccountExpectedBalance.compareTo(receiverAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(BigDecimal transferAmount, String errorValue) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = UserSteps.getAccounts(username, password);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, receiverAccountId);

        //ожидаем что баланс 1 и 2 счета не изменились
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup;
        BigDecimal receiverAccountExpectedBalance = receiverAccountBalanceAfterSetup;

        //првоеряем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, receiverAccountExpectedBalance.compareTo(receiverAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(BigDecimal transferAmount) {
        //создаем 2го пользователя, логинимся под ним, создаем счет
        var createUserRequest = AdminSteps.createUser();
        var secondUserAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        long receiverUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //запрос на трансфер
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverUserAccountId)
                .amount(transferAmount)
                .build();

        new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета 1 пользователя
        var userAccounts = UserSteps.getAccounts(username, password);

        //получаем счета 2 пользователя
        var secondUserAccountsResponse = UserSteps.getAccounts(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondAccountBalanceAfterTransfer = UserSteps.getAccountBalance(secondUserAccountsResponse, receiverUserAccountId);

        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup.subtract(transferAmount);
        //ожидаем что на счете 2го пользователя теперь баланс стал больше на сумму трансфера
        BigDecimal secondUserExpectedBalance = secondAccountInitialBalance.add(transferAmount);

        //проверяем баланс счета 1го пользователя
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс счета 2го пользователя
        assertEquals(0, secondUserExpectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(BigDecimal transferAmount, String errorValue) {
        //создаем 2го пользователя, логинимся под ним, создаем счет
        var createUserRequest = AdminSteps.createUser();
        var secondUserAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //трансфер денег
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

    //получаем счета 1 пользователя
        var userAccounts = UserSteps.getAccounts(username, password);

        //получаем счета 2 пользователя
        var secondUserAccountsResponse = UserSteps.getAccounts(
                createUserRequest.getUsername(),
                createUserRequest.getPassword()
        );

        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondUserAccountBalanceAfterTransfer = UserSteps.getAccountBalance(secondUserAccountsResponse, secondUserAccountId);

        //ожидаем что баланс счета 1 пользователя не изменился
        BigDecimal senderAccountExpectedBalance = senderAccountBalanceAfterSetup;
        //ожидаем что баланс счета 1 пользователя не изменился
        BigDecimal secondUserExpectedBalance = secondAccountInitialBalance;

        //првоеряем баланс 1 счета
        assertEquals(0, senderAccountExpectedBalance.compareTo(senderAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, secondUserExpectedBalance.compareTo(secondUserAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @CsvSource({
            "100, Invalid transfer: insufficient funds or invalid accounts"
    })
    public void userCanNotTransferWhenAmountMoreThanBalance(BigDecimal transferAmount, String errorValue) {
        //готовим данные для трансфера
        //счета поменяны местами, чтобы с нулевого переводить на счет с деньгами
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(receiverAccountId)
                .receiverAccountId(senderAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = UserSteps.getAccounts(username, password);

        //вытаскиваем баланс со второго счета
        BigDecimal receiverBalanceAfterTransfer = UserSteps.getAccountBalance(userAccounts, receiverAccountId);
        BigDecimal expectedBalance = receiverAccountBalanceAfterSetup;

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(receiverBalanceAfterTransfer));
    }
}
