package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Настройка мока сервиса фрод-проверки на время одного теста.
 * <p>
 * Поднимает и настраивает мок {@code FraudCheckWireMockExtension}. Все кейсы
 * различаются только значениями этой аннотации — тело теста при этом не меняется.
 * <p>
 * Поля делятся на две группы:
 * <ul>
 *   <li>{@link #behaviour()} + {@link #httpStatus()} + {@link #delayMillis()} — КАК
 *       ведёт себя сервис: отвечает, отдаёт ошибку, рвёт соединение, лежит;</li>
 *   <li>остальные — ЧТО он отвечает, когда {@code behaviour = RESPOND}.</li>
 * </ul>
 * <p>
 * <b>Важно: здесь описан контракт фрод-сервиса, а не ответ nbank.</b> Тест их не
 * видит напрямую: он зовёт nbank, nbank зовёт этот мок и на основе его ответа
 * собирает свой. Поэтому наборы полей не совпадают, а одноимённое поле {@code status}
 * в двух контрактах означает разное:
 * <pre>
 *   status здесь          — отработала ли сама проверка (SUCCESS / ошибка)
 *   status в ответе nbank — что стало с переводом (APPROVED / BLOCKED / ...)
 * </pre>
 * Ответ nbank строится так: {@code decision} превращается в его {@code status} и
 * {@code message}, {@code riskScore} -> {@code fraudRiskScore},
 * {@code reason} -> {@code fraudReason},
 * {@code additionalVerificationRequired} -> {@code requiresVerification};
 * {@code status} в ответе nbank не появляется вовсе — см. оговорку у {@link #status()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface FraudCheckMock {

    /**
     * Как ведёт себя сервис фрод-проверки в этом тесте.
     */
    enum Behaviour {
        /** Отвечает 200 и телом, собранным из полей аннотации. */
        RESPOND,
        /** Отвечает не-200 (код задаётся в {@link FraudCheckMock#httpStatus()}). */
        HTTP_ERROR,
        /** Рвёт TCP-соединение, не ответив. */
        CONNECTION_ERROR,
        /** Сервис не поднят вовсе — бэк получает connection refused. */
        SERVICE_DOWN
    }

    /**
     * Поведение сервиса. По умолчанию — штатный ответ.
     */
    Behaviour behaviour() default Behaviour.RESPOND;

    /**
     * Код ответа для {@link Behaviour#HTTP_ERROR}. На остальные поведения не влияет.
     */
    int httpStatus() default 500;

    /**
     * Задержка перед ответом, мс. Нужна, чтобы воспроизвести таймаут на стороне бэка.
     * 0 — без задержки.
     */
    int delayMillis() default 0;

    /**
     * Статус самой фрод-проверки: отработала она или нет. НЕ вердикт по переводу —
     * за него отвечает {@link #decision()}.
     * <p>
     * Влияет ли это поле на решение бэка — не проверено. Ответ он принимает, если
     * пришёл 200 и тело не пустое; дальше status уезжает внутрь его FraudCheckResult,
     * и там значение "SUCCESS" встречается. Выяснится тестом со {@code status = "ERROR"}.
     */
    String status() default "SUCCESS";

    /**
     * Вердикт фрод-сервиса. Из него nbank выводит статус перевода в своём ответе.
     */
    String decision() default "APPROVED";

    /**
     * The risk score (0.0 to 1.0)
     */
    double riskScore() default 0.2;

    /**
     * The reason for the fraud check result
     */
    String reason() default "Low risk transaction";

    /**
     * Whether manual review is required
     */
    boolean requiresManualReview() default false;

    /**
     * Whether additional verification is required
     */
    boolean additionalVerificationRequired() default false;

    /**
     * Порт, на котором поднимается мок.
     * <p>
     * Это не свободный параметр: бэк ищет фрод-сервис по адресу из своей property
     * {@code fraud.detection.service.url} (в compose она задаётся переменной
     * {@code FRAUD_DETECTION_SERVICE_URL}). Значения обязаны совпадать — иначе мок
     * поднимется, бэк до него не достучится и уйдёт в аварийную ветку. Менять только
     * вместе с docker-compose.yml.
     */
    int port() default 8080;

    /**
     * Путь, который мокаем. Задан бэком: он делает {@code POST {url}/fraud-check}.
     * Как и {@link #port()}, свободным параметром не является.
     */
    String endpoint() default "/fraud-check";
}
