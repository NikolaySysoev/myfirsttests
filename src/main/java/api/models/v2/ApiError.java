package api.models.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Тексты ошибок актуальной версии и место, где они лежат в теле ответа.
 * <p>
 * Новая версия отвечает JSON'ом, но в двух разных формах:
 * <ul>
 *     <li>бизнес-ошибка — {@code {"message": "..."}}, путь {@code message};</li>
 *     <li>ошибка валидации — {@code {"amount": ["..."]}}, путь {@code amount[0]}.</li>
 * </ul>
 * Поэтому путь задаётся у каждой константы отдельно, а не один на всю версию.
 */
@Getter
@AllArgsConstructor
public enum ApiError {

    // --- проверено на живом бэке ---
    DEPOSIT_LOWER_BOUNDARY("message", "Invalid account or amount"),
    DEPOSIT_HIGHER_BOUNDARY("message", "Deposit amount exceeds the 5000 limit"),
    DEPOSIT_FORBIDDEN("message", "Unauthorized access to account"),
    // нижняя граница у депозита и трансфера отдаётся одной и той же валидацией поля amount
    TRANSFER_LOWER_BOUNDARY("message", "must be greater than 0"),

    // --- НЕ проверено: тексты и пути перенесены из легаси, уточнить при первом прогоне
    //     соответствующих тестов на V2 ---
    TRANSFER_HIGHER_BOUNDARY("message", "Transfer amount cannot exceed 10000"),
    TRANSFER_INSUFFICIENT_FUNDS("message", "Invalid transfer: insufficient funds or invalid accounts"),
    CHANGE_NAME_ERROR("message", "Name must contain two words with letters only");

    private final String jsonPath;
    private final String message;
}
