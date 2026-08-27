package common.annotations;

import api.configs.BackendVersion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Пин теста на конкретную версию бэкенда.
 * <p>
 * Приоритет разрешения версии (см. {@code ApiVersionExtension}):
 * <pre>
 *   @ApiVersion на методе -> @ApiVersion на классе -> -DbackendVersion -> V2
 * </pre>
 * Без аннотации тест идёт на версию по умолчанию, то есть на актуальный бэк.
 * Аннотация нужна только там, где тест нужно принудительно оставить на легаси.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ApiVersion {
    BackendVersion value();
}
