package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement recipientNameInput = $(Selectors.byAttribute("placeholder", "Enter recipient name"));
    private SelenideElement recipientAccountInput = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
    private SelenideElement enterAmountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement checkbox = $(Selectors.byId("confirmCheck"));
    private SelenideElement transferButton = $(Selectors.byText("\uD83D\uDE80 Send Transfer"));

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage checkbox(boolean isChecked) {
        if (isChecked) {
            checkbox.click();
            checkbox.shouldBe(Condition.checked);
        }
        return this;
    }
}
