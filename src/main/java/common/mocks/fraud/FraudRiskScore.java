package common.mocks.fraud;

/**
 * Значения {@code riskScore} (0.0–1.0), которые мок фрод-сервиса отдаёт в тестах.
 * nbank переносит их без изменений в {@code fraudRiskScore} своего ответа.
 * <p>
 * Константы, а не енам: значение атрибута аннотации должно быть константой
 * времени компиляции, а поле енама ею не является. Имена совпадают с {@link FraudReason}.
 */
public final class FraudRiskScore {

    public static final double LOW_RISK = 0.2;
    public static final double HIGH_RISK = 0.95;
    public static final double SUSPICIOUS_ACTIVITY = 0.6;
    public static final double VERIFICATION_NEEDED = 0.45;

    private FraudRiskScore() {
    }
}
