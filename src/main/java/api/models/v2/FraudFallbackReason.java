package api.models.v2;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@code fraudReason} в ответе nbank, когда фрод-сервис не дал вердикта и бэк ушёл
 * в аварийную ветку ({@link TransferStatus#MANUAL_REVIEW_REQUIRED}).
 * <p>
 * Тексты генерирует сам nbank, а не фрод-сервис, поэтому они здесь, а не в
 * {@code FraudReason}. Сверяются по префиксу: бэк может дописать детали.
 */
@Getter
@AllArgsConstructor
public enum FraudFallbackReason {
    /** Фрод-сервис ответил не-200; бэк дописывает код ответа сразу после префикса. */
    UNEXPECTED_ERROR("Unexpected error during fraud check: "),
    /** Соединение порвано или сервис не поднят. */
    SERVICE_UNAVAILABLE("Fraud detection service is currently unavailable");

    private final String text;

    /** Префикс для {@link #UNEXPECTED_ERROR} с кодом ответа, например {@code ...: 500}. */
    public String withHttpStatus(int httpStatus) {
        return text + httpStatus;
    }
}
