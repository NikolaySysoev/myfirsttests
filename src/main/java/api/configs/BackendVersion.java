package api.configs;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Версия бэкенда, на которой выполняется тест.
 * <p>
 * Здесь и только здесь живёт знание о том, где физически стоит каждая версия
 * (порт), какой префикс пути она использует и есть ли у неё внешнее хранилище.
 * Добавление новой версии — одна строка в этом enum'е; все switch'и по версии
 * становятся неисчерпывающими, и компилятор сам покажет места, которые нужно
 * дописать.
 */
@Getter
@AllArgsConstructor
public enum BackendVersion {

    /**
     * Легаси-бэк. Данные держит в памяти, внешней БД у него нет — поэтому
     * {@code dbUrl} равен null, и проверки на уровне БД для этой версии
     * невыполнимы (см. {@code DbChecks}).
     */
    V1(4112, "/api/v1", null),

    /** Актуальный бэк. Версия по умолчанию, данные лежат в postgres. */
    V2(4111, "/api/v1", "jdbc:postgresql://localhost:5433/nbank");

    private final int port;
    private final String basePath;

    /** JDBC-адрес хранилища этой версии, либо null, если внешнего хранилища нет. */
    private final String dbUrl;

    /** Полный базовый URI для RestAssured: хост + порт + префикс пути. */
    public String baseUri() {
        return "http://localhost:" + port + basePath;
    }

    /**
     * Есть ли у версии внешнее хранилище, к которому можно ходить из тестов.
     * <p>
     * Это свойство версии, а не настройка: у легаси базы нет физически, и никакой
     * конфигурацией её не появится.
     */
    public boolean hasDatabase() {
        return dbUrl != null;
    }
}
