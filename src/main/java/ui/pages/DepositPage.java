package ui.pages;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class DepositPage extends BasePage<DepositPage> {
    private SelenideElement depositPageText = $(Selectors.byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement depositButton = $(Selectors.byText("\uD83D\uDCB5 Deposit"));
    private SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement accountSelector = $("select.account-selector");

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage chooseAccount(int accNumber) {
        accountSelector.selectOption(accNumber);
        return this;
    }
}
