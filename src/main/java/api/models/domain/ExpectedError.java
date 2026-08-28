package api.models.domain;

import lombok.Data;

/**
 * Как ожидаемая ошибка выглядит в теле ответа конкретной версии.
 *
 * @see #getJsonPath()
 */
@Data
public class ExpectedError {

    /**
     * Путь до текста ошибки внутри JSON-тела, либо {@code null}, если тело ответа —
     * это и есть текст (так отвечает легаси).
     * <p>
     * У новой версии путей два вида: бизнес-ошибки лежат в {@code message},
     * а ошибки валидации — в поле с именем невалидного параметра, списком:
     * {@code {"amount": ["must be greater than 0"]}} -> путь {@code amount[0]}.
     */
    private final String jsonPath;

    /** Ожидаемый текст ошибки в этой версии. */
    private final String message;

    /** Ошибка, тело которой целиком является текстом (легаси). */
    public static ExpectedError plainBody(String message) {
        return new ExpectedError(null, message);
    }

    /** Ошибка, текст которой лежит в JSON по указанному пути. */
    public static ExpectedError atPath(String jsonPath, String message) {
        return new ExpectedError(jsonPath, message);
    }
}
