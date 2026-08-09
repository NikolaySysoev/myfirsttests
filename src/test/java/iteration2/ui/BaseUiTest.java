package iteration2.ui;

import api.models.requests.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import iteration2.api.BaseTest;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = api.configs.Config.getProperty("uiRemote");
        Configuration.baseUrl = api.configs.Config.getProperty("uiBaseUrl");
        Configuration.browser = api.configs.Config.getProperty("uiBrowser");
        Configuration.browserSize = api.configs.Config.getProperty("uiBrowserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true, "enablevideo", false)
        );
    }

    public void putUserTokenInLocalStorage(String username, String password) {
        Selenide.open("/login");
        String authToken = RequestSpecs.getUserAuthHeader(username,password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);
    }

    public void putUserTokenInLocalStorage(CreateUserRequest createUserRequest) {
        putUserTokenInLocalStorage(createUserRequest.getUsername(), createUserRequest.getPassword());
    }
}
