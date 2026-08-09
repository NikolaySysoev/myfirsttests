package iteration2.ui;

import api.generators.RandomData;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import com.codeborne.selenide.Selenide;
import iteration2.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlerts;
import ui.pages.TransferPage;

import java.math.BigDecimal;
import java.util.ArrayList;

import static iteration2.TestUtils.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest extends BaseUiTest {
    private final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");
    private final String DEFAULT_NAME = "Noname";

    ArrayList<String> userAccountsNumbers = new ArrayList<>();
    ArrayList<Long> userAccountsIds = new ArrayList<>();

    private final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());
    private final BigDecimal invalidBalance = new BigDecimal("0");
    private final String invalidRecipientAccountId = RandomData.getRandomAmountAsString();
    private BigDecimal senderInitialBalance;
    private String username;
    private String password;
    private Long senderAccountId;
    private Long recipientAccountId;
    private String recipientAccountNumber;

    TransferPage transferPage = new TransferPage();

    @BeforeEach
    public void Setup() {
        // админ создает пользователя
        var userData = AdminSteps.createUser();

        username = userData.getUsername();
        password = userData.getPassword();

        putUserTokenInLocalStorage(username,password);

        // создаем 2 аккаунта
        repeat(2, () -> {
            var userAccount = UserSteps.createAccount(username, password);
            userAccountsNumbers.add(userAccount.getAccountNumber());
            userAccountsIds.add(userAccount.getId());
        });

        senderAccountId = userAccountsIds.getFirst();
        recipientAccountId = userAccountsIds.get(1);
        recipientAccountNumber = userAccountsNumbers.get(1);

        // депозит на счет
        UserSteps.depositMoney(senderAccountId, DEFAULT_DEPOSIT, username, password);

        // сохраняем баланс для будущих проверок
        var userAccounts = UserSteps.getAccounts(username, password);
        senderInitialBalance = TestUtils.getAccountBalance(userAccounts, senderAccountId);
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    public void UserCanTransfer() {
        String expectedAlert = BankAlerts.USER_TRANSFER_SUCCESS.format(randomBalance, userAccountsNumbers.get(1));

        transferPage.open()
                .chooseAccount(senderAccountId)
                .setValue(transferPage.getRecipientNameInput(), DEFAULT_NAME)
                .setValue(transferPage.getRecipientAccountInput(), recipientAccountNumber)
                .setValue(transferPage.getEnterAmountInput(), String.valueOf(randomBalance))
                .checkbox(true)
                .click(transferPage.getTransferButton())
                .checkAlertMessageAndAccept(expectedAlert);

        //Проверка на API
        var userAccounts = UserSteps.getAccounts(username, password);
        var recipientAccountBalance = TestUtils.getAccountBalance(userAccounts, recipientAccountId);
        assertEquals(0, recipientAccountBalance.compareTo(randomBalance));
    }

    @Test
    public void UserCanNotTransferWhenEmptyFields() {
        transferPage.open()
                .chooseAccount(senderAccountId)
                .setValue(transferPage.getRecipientNameInput(), DEFAULT_NAME)
                .setValue(transferPage.getRecipientAccountInput(), recipientAccountNumber)
                .setValue(transferPage.getEnterAmountInput(), String.valueOf(randomBalance))
                .checkbox(false)
                .click(transferPage.getTransferButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_TRANSFER_FAIL_EMPTY_FORM.getMessage());

        //Проверка на API (баланс пользователя не изменился)
        var userAccounts = UserSteps.getAccounts(username, password);
        var senderAccountBalance = TestUtils.getAccountBalance(userAccounts, senderAccountId);
        assertEquals(0, senderAccountBalance.compareTo(senderInitialBalance));
    }

    @Test
    public void UserCanNotTransferWhenInvalidRecipientAccount() {
        transferPage.open()
                .chooseAccount(senderAccountId)
                .setValue(transferPage.getRecipientNameInput(), DEFAULT_NAME)
                .setValue(transferPage.getRecipientAccountInput(), invalidRecipientAccountId)
                .setValue(transferPage.getEnterAmountInput(), String.valueOf(randomBalance))
                .checkbox(true)
                .click(transferPage.getTransferButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_TRANSFER_FAIL_INVALID_ACCOUNT.getMessage());

        //Проверка на API (баланс пользователя не изменился)
        var userAccounts = UserSteps.getAccounts(username, password);
        var senderAccountBalance = TestUtils.getAccountBalance(userAccounts, senderAccountId);
        assertEquals(0, senderAccountBalance.compareTo(senderInitialBalance));
    }

    @Test
    public void UserCanNotTransferWhenInvalidAmount() {
        transferPage.open()
                .chooseAccount(senderAccountId)
                .setValue(transferPage.getRecipientNameInput(), DEFAULT_NAME)
                .setValue(transferPage.getRecipientAccountInput(), recipientAccountNumber)
                .setValue(transferPage.getEnterAmountInput(), String.valueOf(invalidBalance))
                .checkbox(true)
                .click(transferPage.getTransferButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_TRANSFER_FAIL_INVALID_AMOUNT_LOWER_001.getMessage());

        //Проверка на API (баланс пользователя не изменился)
        var userAccounts = UserSteps.getAccounts(username, password);
        var senderAccountBalance = TestUtils.getAccountBalance(userAccounts, senderAccountId);
        assertEquals(0, senderAccountBalance.compareTo(senderInitialBalance));
    }
}
