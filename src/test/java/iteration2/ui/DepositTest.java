package iteration2.ui;

import api.generators.RandomData;
import com.codeborne.selenide.Condition;
import common.annotations.UserSession;
import common.storage.SessionStorage;
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

    DepositPage depositPage = new DepositPage();

    @BeforeEach
    public void setup() {
        // пользователь уже создан и залогинен UiUserSessionExtension'ом (по @UserSession на тестовом методе)
        var userAccount = SessionStorage.actAsUser().createAccount();
        accountNumber = userAccount.getAccountNumber();
        accountId = userAccount.getId();
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
        var userBalance = SessionStorage.actAsUser().getAccountBalance(accountId);
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
        var userBalance = SessionStorage.actAsUser().getAccountBalance(accountId);
        assertEquals(0, BigDecimal.ZERO.compareTo(userBalance));
    }
}
