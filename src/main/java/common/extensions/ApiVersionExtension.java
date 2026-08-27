package common.extensions;

import api.configs.BackendVersion;
import api.models.factory.DtoFactory;
import common.annotations.ApiVersion;
import common.versioning.ApiVersionContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Определяет версию бэкенда для каждого теста и публикует её в
 * {@link ApiVersionContext}.
 * <p>
 * Порядок разрешения:
 * <ol>
 *     <li>{@code -DbackendVersion.force=V1} — принудительно гонит весь прогон
 *         на одной версии, игнорируя аннотации. Нужен для регресса легаси
 *         целиком, без правки кода тестов.</li>
 *     <li>{@link ApiVersion} на тестовом методе</li>
 *     <li>{@link ApiVersion} на классе теста</li>
 *     <li>{@code -DbackendVersion=V1} — версия по умолчанию для прогона</li>
 *     <li>{@link BackendVersion#V2} — дефолт в коде</li>
 * </ol>
 * <p>
 * ВАЖНО: экстеншен должен быть зарегистрирован раньше тех, что ходят в API
 * (например {@code ApiUserSessionExtension}), иначе они стартуют до того,
 * как версия определена. JUnit вызывает {@code BeforeEachCallback} в порядке
 * регистрации.
 * <p>
 * Дополнительно работает как {@link ParameterResolver}: тест может принять
 * {@link DtoFactory} или {@link BackendVersion} параметром вместо обращения
 * к статическому контексту.
 */
public class ApiVersionExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final String VERSION_PROPERTY = "backendVersion";
    private static final String FORCE_PROPERTY = "backendVersion.force";
    private static final BackendVersion FALLBACK = BackendVersion.V2;

    @Override
    public void beforeEach(ExtensionContext context) {
        ApiVersionContext.set(resolve(context));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ApiVersionContext.clear();
    }

    private BackendVersion resolve(ExtensionContext context) {
        BackendVersion forced = read(FORCE_PROPERTY);
        if (forced != null) {
            return forced;
        }

        return AnnotationSupport.findAnnotation(context.getElement(), ApiVersion.class)
                .or(() -> AnnotationSupport.findAnnotation(context.getRequiredTestClass(), ApiVersion.class))
                .map(ApiVersion::value)
                .orElseGet(() -> {
                    BackendVersion fromFlag = read(VERSION_PROPERTY);
                    return fromFlag != null ? fromFlag : FALLBACK;
                });
    }

    private BackendVersion read(String property) {
        String raw = System.getProperty(property);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BackendVersion.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Неизвестное значение -D" + property + "=" + raw
                            + ". Допустимые: V1, V2", e);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == DtoFactory.class || type == BackendVersion.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == DtoFactory.class
                ? ApiVersionContext.dto()
                : ApiVersionContext.current();
    }
}
