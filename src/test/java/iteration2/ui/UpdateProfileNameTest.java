package iteration2.ui;

import com.codeborne.selenide.Selenide;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlerts;
import ui.pages.EditProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UpdateProfileNameTest extends BaseUiTest{
    private final String NEW_VALID_NAME = "Valid Name";
    private final String NEW_INVALID_NAME = "newInvalidName";

    private String userInitialName;

    EditProfilePage editProfilePage = new EditProfilePage();

    @BeforeEach
    public void Setup() {
        // пользователь уже создан и залогинен UiUserSessionExtension'ом (по @UserSession на тестовом методе)
        userInitialName = SessionStorage.actAsUser().getCustomerProfile().getName();
    }

    @UserSession
    public void UserCanChangeName() {
        editProfilePage.open()
                .setValue(editProfilePage.getNewNameInput(), NEW_VALID_NAME)
                .click(editProfilePage.getSaveChangesButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_CHANGE_NAME_SUCCESS.getMessage());

        // Проверка на API
        String actualUserName = SessionStorage.actAsUser().getCustomerProfile().getName();
        assertEquals(NEW_VALID_NAME, actualUserName);
    }

    @Test
    @UserSession
    public void userCanNotChangeNameWhenInvalidNewName() {
        editProfilePage.open()
                .waitAndSetValue(editProfilePage.getNewNameInput(), NEW_INVALID_NAME, 500)
                .click(editProfilePage.getSaveChangesButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_CHANGE_NAME_FAIL.getMessage());

        // Проверка на API
        String actualUserName = SessionStorage.actAsUser().getCustomerProfile().getName();
        assertNotEquals(NEW_INVALID_NAME, actualUserName);
        assertEquals(userInitialName, actualUserName);
    }
}
