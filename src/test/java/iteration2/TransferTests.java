package iteration2;

import generators.RandomData;
import models.UserRole;
import models.requests.*;
import models.responses.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
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

public class TransferTests extends BaseTest {
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");

    private String userAuthToken;
    private long senderAccountId;
    private long receiverAccountId;
    private BigDecimal senderAccountBalanceAfterSetup;
    private BigDecimal receiverAccountBalanceAfterSetup;

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
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

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

        userAuthToken = new CrudRequester (
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginRequest)
                .extract()
                .header("authorization");

        //создание 1го аккаунта (sender)
        var firstAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);

        //создание 2го аккаунта (receiver)
        var secondAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);

        //вытаскиваем айдишки счетов
        senderAccountId = firstAccountResponse.getId();
        receiverAccountId = secondAccountResponse.getId();

        //готовим данные для пополнения счета
        var depositMoneyRequest = DepositMoneyRequest.builder()
                .id(senderAccountId)
                .balance(DEFAULT_DEPOSIT)
                .build();

        //депозитим для будущих трансферов (3 депозита)
        for (int i = 0; i < 3; i++) {
            new ValidatedCrudRequester<DepositMoneyResponse>(
                    RequestSpecs.authAsUser(userAuthToken),
                    Endpoint.DEPOSIT_MONEY,
                    ResponseSpecs.requestReturnsOK()
            )
                    .post(depositMoneyRequest);
        }

        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        senderAccountBalanceAfterSetup = getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        receiverAccountBalanceAfterSetup = getAccountBalance(userAccounts, receiverAccountId);
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
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = getAccountBalance(userAccounts, receiverAccountId);

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
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        BigDecimal senderAccountBalanceAfterTransfer = getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со второго счета
        BigDecimal receiverAccountBalanceAfterTransfer = getAccountBalance(userAccounts, receiverAccountId);

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
        //создаем 2го пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_CREATE_USERS,
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest);

        //логин под вторым пользователем
        var loginUser = LoginRequest.builder()
                .username(createUserRequest.getUsername())
                .password(createUserRequest.getPassword())
                .build();

        String secondUserAuthToken = new CrudRequester(
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginUser)
                .extract()
                .header("authorization");

        //создаем аккаунт второму пользователя
        var secondUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(secondUserAuthToken),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);

        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //запрос на трансфер
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета 1 пользователя
        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //получаем счета 2 пользователя
        var secondUserAccountsResponse = new CrudRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondAccountBalanceAfterTransfer = getAccountBalance(secondUserAccountsResponse, secondUserAccountId);

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
        //создаем 2го пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_CREATE_USERS,
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest);

        //логин под вторым пользователем
        var loginUser = LoginRequest.builder()
                .username(createUserRequest.getUsername())
                .password(createUserRequest.getPassword())
                .build();

        String secondUserAuthToken = new CrudRequester(
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginUser)
                .extract()
                .header("authorization");

        //создаем аккаунт второму пользователя
        var secondUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(secondUserAuthToken),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);

        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        //трансфер денег
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(senderAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

    //получаем счета 1 пользователя
        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //получаем счета 2 пользователя
        var secondUserAccountsResponse = new CrudRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal senderAccountBalanceAfterTransfer = getAccountBalance(userAccounts, senderAccountId);
        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondUserAccountBalanceAfterTransfer = getAccountBalance(secondUserAccountsResponse, secondUserAccountId);

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
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со второго счета
        BigDecimal receiverBalanceAfterTransfer = getAccountBalance(userAccounts, receiverAccountId);

        BigDecimal expectedBalance = receiverAccountBalanceAfterSetup;

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(receiverBalanceAfterTransfer));
    }
}
