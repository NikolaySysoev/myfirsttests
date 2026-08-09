package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboardPage extends BasePage<UserDashboardPage> {
    private SelenideElement depositMoneyButton = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement makeATransferButton = $(Selectors.byText("\uD83D\uDD04 Make a Transfer"));
    private SelenideElement dashboardTitleText = $(Selectors.byText("User Dashboard"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboardPage clickDepositButton() {
        depositMoneyButton.click();
        return this;
    }

}
