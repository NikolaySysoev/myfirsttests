package common.extensions;

import api.models.requests.CreateUserRequest;
import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

import java.util.ArrayList;
import java.util.List;

public class UiUserSessionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        // проверяем, есть ли у теста аннотация UserSession
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation == null) {
            return;
        }

        createUsersAndPopulateStorage(annotation);

        // логиним в браузере (через localStorage) пользователя, указанного в auth()
        BasePage.putUserTokenInLocalStorage(SessionStorage.getUserRawData(annotation.auth()));
    }

    /**
     * Создаёт нужное количество пользователей (annotation.value()) через AdminSteps и кладёт их в SessionStorage.
     * Общая логика для UI и API тестов - используется как этим экстеншеном, так и ApiUserSessionExtension.
     */
    static void createUsersAndPopulateStorage(UserSession annotation) {
        SessionStorage.clear();

        List<CreateUserRequest> users = new ArrayList<>();
        for (int i = 0; i < annotation.value(); i++) {
            users.add(AdminSteps.createUser());
        }

        SessionStorage.addUsers(users);
    }
}
