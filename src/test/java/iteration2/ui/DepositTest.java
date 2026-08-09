package iteration2.ui;

import api.generators.RandomData;
import api.requests.steps.UserSteps;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import common.annotations.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlerts;
import ui.pages.DepositPage;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest extends BaseUiTest {
    private final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());
    private final BigDecimal invalidBalance = new BigDecimal("5001");
    private String accountNumber;
    private long accountId;
    private String username;
    private String password;

    DepositPage depositPage = new DepositPage();

    @BeforeEach
    public void setup() {
        // создаем аккаунт пользователю, который генерится в UserSessionExtension
        var userAccount = UserSteps.createAccount(username, password);
        accountNumber = userAccount.getAccountNumber();
        accountId = userAccount.getId();
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    @UserSession
    public void userCanDepositOnAccount() {
        String expectedAlertText = BankAlerts.USER_DEPOSIT_SUCCESS.format(randomBalance, accountNumber);

        // выбор 1го счета из доступных и заполнение суммы
        depositPage.open()
                .chooseFirstAvailableAccount()
                .setValue(depositPage.getAmountInput(), String.valueOf(randomBalance))
                .getAmountInput().shouldHave(Condition.exactValue(String.valueOf(randomBalance)));

        // клик по кнопке Deposit
        depositPage.click(depositPage.getDepositButton())
                .checkAlertMessageAndAccept(expectedAlertText);

        // проверка на API
        var userAccount = UserSteps.getAccounts(username, password);
        var userBalance = UserSteps.getAccountBalance(userAccount, accountId);
        assertEquals(0, randomBalance.compareTo(userBalance));
    }

    @Test
    @UserSession
    public void userCannotDepositOnAccount() {
        // выбор 1го счета из доступных и заполнение суммы
        depositPage.open()
                .chooseFirstAvailableAccount()
                .setValue(depositPage.getAmountInput(), String.valueOf(invalidBalance))
                .getAmountInput().shouldHave(Condition.exactValue(String.valueOf(invalidBalance)));

        // клик по кнопке Deposit
        depositPage.click(depositPage.getDepositButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_DEPOSIT_FAIL.getMessage());

        // проверка на API
        var userAccount = UserSteps.getAccounts(username, password);
        var userBalance = UserSteps.getAccountBalance(userAccount, accountId);
        assertEquals(0, BigDecimal.ZERO.compareTo(userBalance));
    }
}
