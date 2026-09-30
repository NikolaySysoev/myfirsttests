package common.mocks.fraud;

/**
 * Поле {@code decision} в ответе фрод-сервиса — вердикт по переводу.
 * Из него nbank выводит {@code status} и {@code message} своего ответа.
 * <p>
 * Имя константы уходит в тело мока как есть ({@code name()}).
 */
public enum FraudDecision {
    APPROVED,
    BLOCKED,
    REVIEW_REQUIRED,
    VERIFICATION_REQUIRED
}
