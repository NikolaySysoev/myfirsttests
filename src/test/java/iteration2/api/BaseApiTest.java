package iteration2.api;

import common.extensions.ApiUserSessionExtension;
import common.extensions.TestTimingExtension;
import common.extensions.TestTypeExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Базовый класс для API тестов, которым нужен пользователь из SessionStorage
 * (создаётся ApiUserSessionExtension по аннотации @UserSession на тестовом методе).
 */
@ExtendWith(TestTimingExtension.class)
@ExtendWith(ApiUserSessionExtension.class)
@ExtendWith(TestTypeExtension.class)
public class BaseApiTest {

    protected SoftAssertions softly;

    @BeforeEach
    public void setupTest(){
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest(){
        softly.assertAll();
    }
}
