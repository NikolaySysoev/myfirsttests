package iteration2.ui;

import com.codeborne.selenide.Configuration;
import common.extensions.UserSessionExtension;
import iteration2.api.BaseTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@ExtendWith(UserSessionExtension.class)
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

}
