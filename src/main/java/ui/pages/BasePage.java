package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import java.lang.reflect.Field;

import static com.codeborne.selenide.Selenide.$;

@Getter
public abstract class BasePage<T extends BasePage> {
    private SelenideElement userName = $(Selectors.byClassName("user-username"));
    private SelenideElement userLogin = $(Selectors.byClassName("user-name"));

    public abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public T click(String elementName) {
        getElement(elementName).click();
        return (T) this;
    }

    public T setValue(String elementName, String value) {
        getElement(elementName).setValue(value);
        return (T) this;
    }

    /**
     * Finds a SelenideElement field by its name using reflection.
     *
     * @param elementName field name declared in the page object class or its superclass (BasePage)
     * @return the SelenideElement stored in that field
     * @throws IllegalArgumentException if no such field exists in the class
     * @throws RuntimeException         if the field is not accessible
     */
    private SelenideElement getElement(String elementName) {
        Class<?> clazz = this.getClass();
        Field field = null;

        // поднимаемся по иерархии, пока не найдём поле или не упрёмся в Object
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(elementName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field == null) {
            throw new IllegalArgumentException(
                    "Field '" + elementName + "' not found in class "
                            + this.getClass().getSimpleName() + " or its superclasses"
            );
        }

        try {
            field.setAccessible(true);
            Object value = field.get(this);

            if (!(value instanceof SelenideElement)) {
                throw new IllegalStateException(
                        "Field '" + elementName + "' not a SelenideElement"
                );
            }

            return (SelenideElement) value;

        } catch (IllegalAccessException e) {
            throw new RuntimeException("No access to field '" + elementName + "'", e);
        }
    }
};
