package iteration2.ui;

import api.requests.steps.UserSteps;
import com.codeborne.selenide.Selenide;
import common.annotations.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlerts;
import ui.pages.EditProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UpdateProfileNameTest extends BaseUiTest{
    private final String NEW_VALID_NAME = "Valid Name";
    private final String NEW_INVALID_NAME = "newInvalidName";

    private String userInitialName;
    private String username;
    private String password;

    EditProfilePage editProfilePage = new EditProfilePage();

    @BeforeEach
    public void Setup() {
        userInitialName = UserSteps.getCustomerProfile(username, password).getName();
    }

    @AfterEach
    public void tearDown() {
        Selenide.cookies().clear();
        Selenide.executeJavaScript("localStorage.clear();");
        Selenide.open("about:blank");
    }

    @Test
    @UserSession
    public void UserCanChangeName() {
        editProfilePage.open()
                .setValue(editProfilePage.getNewNameInput(), NEW_VALID_NAME)
                .click(editProfilePage.getSaveChangesButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_CHANGE_NAME_SUCCESS.getMessage());

        // Проверка на API
        String actualUserName = UserSteps.getCustomerProfile(username, password).getName();
        assertEquals(NEW_VALID_NAME, actualUserName);
    }

    @Test
    @UserSession
    public void userCanNotChangeNameWhenInvalidNewName() {
        editProfilePage.open()
                .setValue(editProfilePage.getNewNameInput(), NEW_INVALID_NAME)
                .click(editProfilePage.getSaveChangesButton())
                .checkAlertMessageAndAccept(BankAlerts.USER_CHANGE_NAME_FAIL.getMessage());

        // Проверка на API
        String actualUserName = UserSteps.getCustomerProfile(username, password).getName();
        assertNotEquals(NEW_INVALID_NAME, actualUserName);
        assertEquals(userInitialName, actualUserName);
    }
}

