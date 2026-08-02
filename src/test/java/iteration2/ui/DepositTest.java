package iteration2.ui;

/*
Testuser1
Testuser1!
 */

import com.codeborne.selenide.*;
import generators.RandomData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.math.BigDecimal;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DepositTest {
    private static final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());
    private final BigDecimal invalidBalance = new BigDecimal("5001");
    private String accountNumber;
    private String accountId;
    private String username;
    private String password;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.1.99:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true, "enablevideo", false)
        );
    }

    @BeforeEach
    public void Setup() {
        //1 - админ создает пользователя
        var userData = AdminSteps.createUser();

        username = userData.getUsername();
        password = userData.getPassword();

        //2 - логин под юзер, получение токена
        String authToken = UserSteps.loginUser(userData);

        //3 - открываем страницу логина
        Selenide.open("/login");

        //4 - вставляем токен в localStorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        //5 - переход на страницу /dashboard
        Selenide.open("/dashboard");
        $(Selectors.byText("User Dashboard")).shouldBe(Condition.visible);

        //6 - создаем аккаунт
        $(Selectors.byText("➕ Create New Account")).click();
        Alert alert = Selenide.switchTo().alert();
        String account = alert.getText();
        accountNumber = account.replaceAll(" ", "").split(":")[1];
        accountId = accountNumber.replaceAll("\\D", "");
        alert.accept();


//        repeat(2, () -> {
//                    $(Selectors.byText("➕ Create New Account")).click();
//                    Alert alert = Selenide.switchTo().alert();
//                    alert.accept();
//                }
//        );
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    public void userCanDepositOnAccount() {
        // клик по кнопке Deposit Money + проверка перехода на нужный экран
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).shouldBe(Condition.visible);

        // выбор 1го счета из доступных
        SelenideElement accountSelector = $("select.account-selector");
        accountSelector.selectOption(1);

        // заполнение инпут поля Enter Amount
        SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
        amountInput.clear();
        amountInput.setValue(String.valueOf(randomBalance));
        amountInput.shouldHave(Condition.exactValue(String.valueOf(randomBalance)));

        // клик по кнопке Deposit
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).click();

        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();

        // Проверка на UI через сообщение в Alert
        assertEquals("✅ Successfully deposited $" + randomBalance + " to account " + accountNumber + "!", alertText);

        // проверка на API
        var userAccount = UserSteps.getAccounts(username, password);
        var userBalance = UserSteps.getAccountBalance(userAccount, Long.parseLong(accountId));
        assertEquals(0, randomBalance.compareTo(userBalance));
    }

    @Test
    public void UserCanNotDepositOnAccount() {
        // клик по кнопке Deposit Money + проверка перехода на нужный экран
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).click();
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).shouldBe(Condition.visible);

        // выбор 1го счета из доступных (единственный доступный, поэтому можно через selectOption)
        SelenideElement accountSelector = $("select.account-selector");
        accountSelector.selectOption(1);

        // заполнение инпут поля Enter Amount
        SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
        amountInput.clear();
        amountInput.setValue(String.valueOf(invalidBalance));
        amountInput.shouldHave(Condition.exactValue(String.valueOf(invalidBalance)));

        // клик по кнопке Deposit
        $(Selectors.byText("\uD83D\uDCB5 Deposit")).click();

        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();

        // Проверка на UI через сообщение в Allert
        assertEquals("❌ Please deposit less or equal to 5000$.", alertText);

        // проверка на API
        var userAccount = UserSteps.getAccounts(username, password);
        var userBalance = UserSteps.getAccountBalance(userAccount, Long.parseLong(accountId));
        assertNotEquals(0, invalidBalance.compareTo(userBalance));
    }
}
