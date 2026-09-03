package common.extensions;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import common.annotations.FraudCheckMock;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Поднимает мок сервиса фрод-проверки на время теста по настройкам {@link FraudCheckMock}.
 * <p>
 * Заглушка регистрируется у конкретного экземпляра сервера
 * ({@code wireMockServer.stubFor}), а не через статический клиент WireMock:
 * статический клиент — глобальное состояние на всю JVM, и при нескольких
 * мок-серверах или параллельном прогоне заглушка может уехать не туда.
 */
public class FraudCheckWireMockExtension implements BeforeEachCallback, AfterEachCallback {

    private WireMockServer wireMockServer;

    @Override
    public void beforeEach(ExtensionContext context) {
        // настройка мока берётся с метода, иначе — с класса
        FraudCheckMock mockConfig = context.getTestMethod()
                .map(method -> method.getAnnotation(FraudCheckMock.class))
                .orElseGet(() -> context.getTestClass()
                        .map(clazz -> clazz.getAnnotation(FraudCheckMock.class))
                        .orElse(null));

        if (mockConfig != null) {
            setupWireMock(mockConfig);
        }
    }

    private void setupWireMock(FraudCheckMock config) {
        // «сервис лежит»: сервер не поднимаем вообще, бэк получит connection refused
        if (config.behaviour() == FraudCheckMock.Behaviour.SERVICE_DOWN) {
            return;
        }

        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(config.port()));
        wireMockServer.start();

        // что именно отдаст мок — определяется поведением из аннотации
        var response = switch (config.behaviour()) {
            case RESPOND -> aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(buildResponseBody(config));
            case HTTP_ERROR -> aResponse()
                    .withStatus(config.httpStatus())
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"fraud detection service failure\"}");
            // соединение рвётся без ответа — для бэка это I/O-ошибка, а не код ответа
            case CONNECTION_ERROR -> aResponse()
                    .withFault(Fault.CONNECTION_RESET_BY_PEER);
            case SERVICE_DOWN -> throw new IllegalStateException(
                    "SERVICE_DOWN обрабатывается до создания сервера");
        };

        // задержка нужна, чтобы воспроизвести таймаут на стороне бэка
        if (config.delayMillis() > 0) {
            response = response.withFixedDelay(config.delayMillis());
        }

        wireMockServer.stubFor(post(urlPathMatching(config.endpoint()))
                .willReturn(response));
    }

    /**
     * Тело штатного ответа фрод-сервиса.
     * <p>
     * riskScore подставляется через %s, а не %.1f, по двум причинам:
     * %.1f берёт системную локаль и на ru_RU пишет дробь через запятую ("0,2") —
     * тело становится невалидным JSON, бэк не может его разобрать и уходит в
     * аварийную ветку "требуется ручная проверка"; плюс %.1f округляет, и 0.95
     * из аннотации приехало бы в мок как 1.0. Locale.ROOT оставлен на весь шаблон,
     * чтобы локаль не влияла на формат и при будущих правках.
     */
    private String buildResponseBody(FraudCheckMock config) {
        return String.format(Locale.ROOT, "{\n" +
                        "  \"status\": \"%s\",\n" +
                        "  \"decision\": \"%s\",\n" +
                        "  \"riskScore\": %s,\n" +
                        "  \"reason\": \"%s\",\n" +
                        "  \"requiresManualReview\": %s,\n" +
                        "  \"additionalVerificationRequired\": %s\n" +
                        "}",
                config.status(),
                config.decision(),
                config.riskScore(),
                config.reason(),
                config.requiresManualReview(),
                config.additionalVerificationRequired());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}
