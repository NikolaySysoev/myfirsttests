package common.mocks.fraud;

/**
 * Поле {@code status} в ответе фрод-сервиса: отработала ли сама проверка.
 * НЕ вердикт по переводу — за него отвечает {@link FraudDecision}.
 * <p>
 * Имя константы уходит в тело мока как есть ({@code name()}).
 */
public enum FraudCheckStatus {
    SUCCESS,
    // НЕ проверено: как бэк реагирует на такой статус, выяснится отдельным тестом
    ERROR
}
