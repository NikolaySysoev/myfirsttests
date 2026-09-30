package api.models.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Итог перевода с фрод-проверкой в ответе nbank: пара {@code status} + {@code message}.
 * <p>
 * Не путать с {@code FraudDecision} — это контракт фрод-сервиса. nbank выводит
 * свой статус из его {@code decision}, но значения не совпадают один в один:
 * {@code REVIEW_REQUIRED} у фрод-сервиса -> {@code MANUAL_REVIEW_REQUIRED} здесь.
 * Сюда же nbank уходит, когда фрод-сервис недоступен или ответил ошибкой.
 */
@Getter
@AllArgsConstructor
public enum TransferStatus {
    APPROVED("APPROVED", "Transfer approved and processed immediately"),
    BLOCKED("BLOCKED", "Transfer blocked due to fraud detection"),
    MANUAL_REVIEW_REQUIRED("MANUAL_REVIEW_REQUIRED", "Transfer requires manual review"),
    VERIFICATION_REQUIRED("VERIFICATION_REQUIRED", "Additional verification required");

    private final String status;
    private final String message;
}
