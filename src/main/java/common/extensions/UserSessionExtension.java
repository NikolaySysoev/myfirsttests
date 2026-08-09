package common.extensions;

import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

import java.lang.reflect.Field;

public class UserSessionExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        // проверяем есть ли у теста аннотация UserSession
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation == null) {
            return;
        }

        // 1 - админ создаёт пользователя
        var userData = AdminSteps.createUser();

        // 2 - записываем логин/пароль в поля тестового класса для будущего использования
        Object testInstance = extensionContext.getRequiredTestInstance();
        setFieldValue(testInstance, "username", userData.getUsername());
        setFieldValue(testInstance, "password", userData.getPassword());

        // 3 - логинимся под этим юзером
        BasePage.putUserTokenInLocalStorage(userData.getUsername(), userData.getPassword());
    }

    private void setFieldValue(Object instance, String fieldName, String value) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
