package ui.pages;

import api.models.requests.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import static com.codeborne.selenide.Selenide.$;

@Getter
public abstract class BasePage<T extends BasePage> {
    protected SelenideElement userName = $(Selectors.byClassName("user-username"));
    protected SelenideElement userLogin = $(Selectors.byClassName("user-name"));
    private SelenideElement accountSelector = $("select.account-selector");

    public abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public static void putUserTokenInLocalStorage(String username, String password) {
        Selenide.open("/login");
        String authToken = RequestSpecs.getUserAuthHeader(username,password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", authToken);
    }

    public static void putUserTokenInLocalStorage(CreateUserRequest createUserRequest) {
        putUserTokenInLocalStorage(createUserRequest.getUsername(), createUserRequest.getPassword());
    }

    public T click(SelenideElement element) {
        element.click();
        return (T) this;
    }

    public T setValue(SelenideElement element, String value) {
        element.clear();
        element.click();
        element.setValue(value);
        element.shouldHave(Condition.exactValue(value));
        return (T) this;
    }

    public T checkAlertMessageAndAccept(String alertMessage){
        Alert alert = Selenide.switchTo().alert();
        String alertText = alert.getText();
        alert.accept();
        assertEquals(alertMessage, alertText);
        return (T) this;
    }

    public T chooseFirstAvailableAccount() {
        accountSelector.selectOption(1);
        return (T) this;
    }

    public T chooseAccount(long accountId) {
        accountSelector.selectOptionContainingText(String.valueOf(accountId));
        return (T) this;
    }
};
