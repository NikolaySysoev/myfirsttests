package common.mocks.fraud;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Поле {@code reason} в ответе фрод-сервиса. nbank переносит его без изменений
 * в {@code fraudReason} своего ответа, поэтому один и тот же текст нужен и в моке,
 * и в ожидаемом ответе теста.
 */
@Getter
@AllArgsConstructor
public enum FraudReason {
    LOW_RISK("Low risk transaction"),
    HIGH_RISK("High risk transaction"),
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
    VERIFICATION_NEEDED("Additional verification needed");

    private final String text;
}
