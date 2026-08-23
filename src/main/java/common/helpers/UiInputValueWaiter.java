package common.helpers;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.UIAssertionError;

public class UiInputValueWaiter {

    public static void waitForValue(SelenideElement element, String value) {
        int tryCount = 0;
        while (true) {
            try {
                element.shouldHave(Condition.exactValue(value));
                return;
            } catch (UIAssertionError e) {
                if (++tryCount > 3) throw e;
                Selenide.sleep(200);
            }
        }
    }
}
