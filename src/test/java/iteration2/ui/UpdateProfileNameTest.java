package iteration2.ui;

import com.codeborne.selenide.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;

import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UpdateProfileNameTest {
    private final String NEW_VALID_NAME = "Valid Name";
    private final String NEW_INVALID_NAME = "newInvalidName";

    private String userInitialName;
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

        //2 - сохраняем имя пользователя после создания
        userInitialName = UserSteps.getCustomerProfile(username, password).getName();

        //3 - логин под юзер, получение токена
        String authToken = UserSteps.loginUser(userData);

        //4 - открываем страницу логина
        Selenide.open("/login");

        //5 - вставляем токен в localStorage
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);

        //6 - переход на страницу /dashboard
        Selenide.open("/dashboard");
        $(Selectors.byText("User Dashboard")).shouldBe(Condition.visible);
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    public void UserCanChangeName() {
        // Клик по логину --> переход на страницу смены имены
        $(Selectors.byText(username)).click();
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(Condition.visible);
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).shouldBe(Condition.visible);

        // Заполение поля Enter New name
        SelenideElement newNameInput = $(Selectors.byAttribute("placeholder", "Enter new name"));
        newNameInput.clear();
        newNameInput.setValue(NEW_VALID_NAME);

        // Клик по Save Changes
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        // Проверка на UI
        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();
        assertEquals("✅ Name updated successfully!", alertText);

        // Проверка на API
        String actualUserName = UserSteps.getCustomerProfile(username, password).getName();
        assertEquals(NEW_VALID_NAME, actualUserName);
    }

    @Test
    public void userCanNotChangeNameWhenInvalidNewName() {
        // Клик по логину --> переход на страницу смены имены
        $(Selectors.byText(username)).click();
        $(Selectors.byText("✏️ Edit Profile")).shouldBe(Condition.visible);
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).shouldBe(Condition.visible);

        // Заполение поля Enter New name
        SelenideElement newNameInput = $(Selectors.byAttribute("placeholder", "Enter new name"));
        newNameInput.clear();
        newNameInput.setValue(NEW_INVALID_NAME);

        // Клик по Save Changes
        $(Selectors.byText("\uD83D\uDCBE Save Changes")).click();

        // Проверка на UI
        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();
        assertEquals("Name must contain two words with letters only", alertText);

        // Проверка на API
        String actualUserName = UserSteps.getCustomerProfile(username, password).getName();
        assertNotEquals(NEW_INVALID_NAME, actualUserName);
        assertEquals(userInitialName, actualUserName);
    }
}

