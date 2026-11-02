package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import common.extensions.BrowserMatchExtension;
import common.extensions.UiUserSessionExtension;
import common.storage.SessionStorage;
import iteration2.api.BaseApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import api.configs.BackendVersion;
import common.annotations.ApiVersion;

@ExtendWith({UiUserSessionExtension.class , BrowserMatchExtension.class})
// Фронт написан под легаси-контракт и ходит в V1 — данные для UI-тестов создаём там же
@ApiVersion(BackendVersion.V1)
public class BaseUiTest extends BaseApiTest {

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = api.configs.Config.getProperty("uiRemote");
        Configuration.baseUrl = api.configs.Config.getProperty("uiBaseUrl");
        Configuration.browser = api.configs.Config.getProperty("uiBrowser");
        Configuration.browserSize = api.configs.Config.getProperty("uiBrowserSize");
        Configuration.fastSetValue = false;
        Configuration.headless = true;

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true, "enablevideo", false)
        );
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }


}
