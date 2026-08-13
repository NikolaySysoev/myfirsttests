package common.extensions;

import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Аналог UiUserSessionExtension для чисто API-тестов: создаёт пользователей и кладёт их в SessionStorage
 */
public class ApiUserSessionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation == null) {
            return;
        }

        UiUserSessionExtension.createUsersAndPopulateStorage(annotation);
    }
}
