package api.models.domain;

/**
 * Логическая ошибка API — ключ, а не текст.
 * <p>
 * Тесты ссылаются только на эти константы: "депозит ниже нижней границы",
 * "чужой счёт" и т.д. Как именно ошибка выглядит в конкретной версии бэкенда —
 * какой текст и в каком месте тела ответа — знает фабрика этой версии
 * ({@code DtoFactory.expect(ApiError)}).
 * <p>
 * Благодаря этому расхождения вида "поменялась формулировка" и "ошибка переехала
 * из голой строки в JSON" не задевают тестовый код.
 */
public enum ApiError {
    DEPOSIT_LOWER_BOUNDARY,
    DEPOSIT_HIGHER_BOUNDARY,
    DEPOSIT_FORBIDDEN,
    TRANSFER_LOWER_BOUNDARY,
    TRANSFER_HIGHER_BOUNDARY,
    TRANSFER_INSUFFICIENT_FUNDS,
    CHANGE_NAME_ERROR
}
