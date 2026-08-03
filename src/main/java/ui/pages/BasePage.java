package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public abstract class BasePage<T extends BasePage> {
    private SelenideElement userName = $(Selectors.byClassName("user-username"));
    private SelenideElement userLogin = $(Selectors.byClassName("user-name"));

    public abstract String url();

    public  T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    ;
};
