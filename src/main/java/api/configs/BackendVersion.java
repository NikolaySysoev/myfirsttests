package api.configs;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Версия бэкенда, на которой выполняется тест.
 * <p>
 * Здесь и только здесь живёт знание о том, где физически стоит каждая версия
 * (порт) и какой префикс пути она использует. Добавление новой версии — одна
 * строка в этом enum'е; все switch'и по версии становятся неисчерпывающими,
 * и компилятор сам покажет места, которые нужно дописать.
 */
@Getter
@AllArgsConstructor
public enum BackendVersion {
    /** Легаси-бэк. Поддерживается ради обратной совместимости тестов. */
    V1(4112, "/api/v1"),
    /** Актуальный бэк. Версия по умолчанию. */
    V2(4111, "/api/v1");

    private final int port;
    private final String basePath;

    /** Полный базовый URI для RestAssured: хост + порт + префикс пути. */
    public String baseUri() {
        return "http://localhost:" + port + basePath;
    }
}
