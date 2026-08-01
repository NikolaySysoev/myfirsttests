package iteration2.ui;

import com.codeborne.selenide.*;
import generators.RandomData;
import iteration2.TestUtils;
import org.junit.jupiter.api.*;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static iteration2.TestUtils.repeat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest {
    private final BigDecimal DEFAULT_DEPOSIT = new BigDecimal("5000");
    private final String DEFAULT_NAME = "Noname";

    private static final BigDecimal randomBalance = new BigDecimal(RandomData.getRandomAmountAsString());
    private final BigDecimal invalidBalance = new BigDecimal("0");
    ArrayList<String> userAccountsNumbers = new ArrayList<>();
    ArrayList<Long> userAccountsIds = new ArrayList<>();
    private String username;
    private String password;
    private Long senderAccountId;
    private Long recipientAccountId;
    private String recipientAccountNumber;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.1.99:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true, "enablevideo", true)
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

        //6 - создаем 2 аккаунта
        repeat(2, () -> {
            var userAccount = UserSteps.createAccount(username, password);
            userAccountsNumbers.add(userAccount.getAccountNumber());
            userAccountsIds.add(userAccount.getId());
        });

        senderAccountId = userAccountsIds.getFirst();
        recipientAccountId = userAccountsIds.get(1);
        recipientAccountNumber = userAccountsNumbers.get(1);


        //7 - депозит на счет
        UserSteps.depositMoney(senderAccountId, DEFAULT_DEPOSIT, username, password);
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    public void UserCanTransfer() {
        // Клик по кнопке Make a Transfer
        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).click();
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).shouldBe(Condition.visible);

        // Выбор аккаунта
        SelenideElement accountSelector = $("select.account-selector");
        accountSelector.selectOptionContainingText(String.valueOf(senderAccountId));

        // Заполнение получателя
        SelenideElement recipientNameInput = $(Selectors.byAttribute("placeholder", "Enter recipient name"));
        recipientNameInput.clear();
        recipientNameInput.setValue(DEFAULT_NAME);
        recipientNameInput.shouldHave(Condition.exactValue(DEFAULT_NAME));

        // Заполнение аккаунта получателя
        SelenideElement recipientAccountInput = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
        recipientAccountInput.clear();
        recipientAccountInput.setValue(String.valueOf(recipientAccountNumber));
        recipientAccountInput.shouldHave(Condition.exactValue(String.valueOf(recipientAccountNumber)));

        // Заполнение суммы перевода
        SelenideElement enterAmountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
        enterAmountInput.clear();
        enterAmountInput.setValue(String.valueOf(randomBalance));
        enterAmountInput.shouldHave(Condition.exactValue(String.valueOf(randomBalance)));

        // Клик по чекбоксу
        SelenideElement checkbox = $(Selectors.byId("confirmCheck"));
        checkbox.click();
        checkbox.shouldBe(Condition.checked);

        // Клик по кнопке перевода
        $(Selectors.byText("\uD83D\uDE80 Send Transfer")).click();

        //Проверка на UI
        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();
        assertEquals("✅ Successfully transferred $" + randomBalance + " to account " + userAccountsNumbers.get(1) + "!", alertText);

        //Проверка на API
        var userAccounts = UserSteps.getAccounts(username, password);
        var recipientAccountBalance = TestUtils.getAccountBalance(userAccounts, recipientAccountId);
        assertEquals(0, recipientAccountBalance.compareTo(randomBalance));
    }
}
