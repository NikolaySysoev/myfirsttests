package iteration2;

import generators.RandomData;
import models.UserRole;
import models.requests.CreateUserRequest;
import models.requests.DepositMoneyRequest;
import models.requests.TransferMoneyRequest;
import models.responses.CreateAccountResponse;
import models.responses.DepositMoneyResponse;
import models.responses.GetUserAccountsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTests extends BaseTest {
    private static final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");

    private String userAuthToken;
    private long firstAccountId;
    private long secondAccountId;
    private BigDecimal firstAccountBalanceAfterDeposit;
    private BigDecimal secondAccountBalanceAfterDeposit;

    @BeforeEach
    public void setup() {
        //готовим данные для создания пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        //создаем пользователя
        userAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");

        //создаем 1 счет
        var firstAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //создаем 2 счет
        var secondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        //вытаскиваем айдишки счетов
        firstAccountId = firstAccountResponse.getId();
        secondAccountId = secondAccountResponse.getId();

        //готовим данные для пополнения счета
        var depositMoneyUserRequest = DepositMoneyRequest.builder()
                .id(firstAccountId)
                .balance(DEFAULT_DEPOSIT)
                .build();

        //депозитим для будущих трансферов (3 депозита)
        for (int i = 0; i < 3; i++) {
            var depositMoneyResponse = new DepositMoneyRequester(
                    RequestSpecs.authAsUser(userAuthToken),
                    ResponseSpecs.requestReturnsOK()
            )
                    .post(depositMoneyUserRequest)
                    .extract()
                    .body()
                    .as(DepositMoneyResponse.class);
        }

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        firstAccountBalanceAfterDeposit = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == firstAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //вытаскиваем баланс со второго счета
        secondAccountBalanceAfterDeposit = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();
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
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        BigDecimal firstAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == firstAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //вытаскиваем баланс со второго счета
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();


        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal firstAccountExpectedBalance = firstAccountBalanceAfterDeposit.subtract(transferAmount);
        //ожидаем что на 2 счете теперь баланс стал больше на сумму трансфера
        BigDecimal secondAccountExpectedBalance = secondAccountBalanceAfterDeposit.add(transferAmount);

        //проверяем баланс 1 счета
        assertEquals(0, firstAccountExpectedBalance.compareTo(firstAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, secondAccountExpectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferBetweenOwnAccountsWhenInvalidAmount(BigDecimal transferAmount, String errorValue) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);

        //вытаскиваем баланс с первого счета
        BigDecimal firstAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == firstAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //вытаскиваем баланс со второго счета
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //ожидаем что баланс 1 и 2 счета не изменились
        BigDecimal firstAccountExpectedBalance = firstAccountBalanceAfterDeposit;
        BigDecimal secondAccountExpectedBalance = secondAccountBalanceAfterDeposit;

        //првоеряем баланс 1 счета
        assertEquals(0, firstAccountExpectedBalance.compareTo(firstAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, secondAccountExpectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("validAmount")
    public void userCanTransferOnOtherUserAccount(BigDecimal transferAmount) {
        //создаем 2го пользователя
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        String secondUserAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");


        //создаем аккаунт второму пользователя
        CreateAccountResponse secondUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .post(transferMoneyRequest);

        //получаем счета 1 пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //получаем счета 2 пользователя
        var secondUserAccountsResponse = new GetAccountsRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal firstAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == firstAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(secondUserAccountsResponse)
                .filter(acc -> acc.getId() == secondUserAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //ожидаем что на 1 счете теперь балланс стал меньше на сумму трансфера
        BigDecimal firstAccountExpectedBalance = firstAccountBalanceAfterDeposit.subtract(transferAmount);
        //ожидаем что на счете 2го пользователя теперь баланс стал больше на сумму трансфера
        BigDecimal expectedBalance = secondAccountInitialBalance.add(transferAmount);

        //проверяем баланс счета 1го пользователя
        assertEquals(0, firstAccountExpectedBalance.compareTo(firstAccountBalanceAfterTransfer));
        //проверяем баланс счета 2го пользователя
        assertEquals(0, expectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @MethodSource("invalidAmount")
    public void userCanNotTransferOnOtherUserAccountWhenInvalidAmount(BigDecimal transferAmount, String errorValue) {
        //создаем 2го пользователя
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        String secondUserAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )

                .post(createUserRequest)
                .extract()
                .header("authorization");

        //создаем счет второму пользователю
        CreateAccountResponse secondUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                ResponseSpecs.entityWasCreated()
        )
                .post(null)
                .extract()
                .body()
                .as(CreateAccountResponse.class);

        long secondUserAccountId = secondUserAccountResponse.getId();
        BigDecimal secondAccountInitialBalance = secondUserAccountResponse.getBalance();

        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(firstAccountId)
                .receiverAccountId(secondUserAccountId)
                .amount(transferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

    //получаем счета 1 пользователя
        var firstUserAccountsResponse = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //получаем счета 2 пользователя
        var secondUserAccountsResponse = new GetAccountsRequester(
                RequestSpecs.authAsUser(secondUserAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со счета 1го пользователя
        BigDecimal firstAccountBalanceAfterTransfer = Arrays.stream(firstUserAccountsResponse)
                .filter(acc -> acc.getId() == firstAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //вытаскиваем баланс со счета 2го пользователя
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(secondUserAccountsResponse)
                .filter(acc -> acc.getId() == secondUserAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        //ожидаем что баланс счета 1 пользователя не изменился
        BigDecimal firstAccountExpectedBalance = firstAccountBalanceAfterDeposit;

        //ожидаем что баланс счета 1 пользователя не изменился
        BigDecimal secondAccountExpectedBalance = secondAccountInitialBalance;

        //првоеряем баланс 1 счета
        assertEquals(0, firstAccountExpectedBalance.compareTo(firstAccountBalanceAfterTransfer));
        //проверяем баланс 2 счета
        assertEquals(0, secondAccountExpectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

    @ParameterizedTest
    @CsvSource({
            "100, Invalid transfer: insufficient funds or invalid accounts"
    })
    public void userCanNotTransferWhenAmountMoreThanBalance(BigDecimal transferAmount, String errorValue) {
        //готовим данные для трансфера
        var transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(secondAccountId)
                .receiverAccountId(firstAccountId)
                .amount(transferAmount)
                .build();

        //делаем трансфер
        new TransferMoneyRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .post(transferMoneyRequest);

        //получаем счета пользователя
        var userAccounts = new GetAccountsRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .body()
                .as(GetUserAccountsResponse[].class);


        //вытаскиваем баланс со второго счета
        BigDecimal secondAccountBalanceAfterTransfer = Arrays.stream(userAccounts)
                .filter(acc -> acc.getId() == secondAccountId)
                .map(acc -> acc.getBalance())
                .findFirst()
                .orElseThrow();

        BigDecimal expectedBalance = secondAccountBalanceAfterDeposit;

        //проверяем баланс 2 счета
        assertEquals(0, expectedBalance.compareTo(secondAccountBalanceAfterTransfer));
    }

}
