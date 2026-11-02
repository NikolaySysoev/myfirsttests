package api.configs;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Версия бэкенда, на которой выполняется тест.
 * <p>
 * Здесь живёт знание о том, под каким ключом конфига лежит адрес каждой версии,
 * какой префикс пути она использует и есть ли у неё внешнее хранилище.
 * Сам адрес (хост + порт) берётся из {@link Config} — его можно переопределить
 * через -D или переменную окружения, например при запуске в Docker.
 * Добавление новой версии — одна строка в этом enum'е; все switch'и по версии
 * становятся неисчерпывающими, и компилятор сам покажет места, которые нужно
 * дописать.
 */
@Getter
@AllArgsConstructor
public enum BackendVersion {

    /**
     * Легаси-бэк. Данные держит в памяти, внешней БД у него нет — поэтому
     * {@code dbUrlKey} равен null, и проверки на уровне БД для этой версии
     * невыполнимы (см. {@code DbChecks}).
     */
    V1("apiBaseUrl.v1", "/api/v1", null),

    /** Актуальный бэк. Версия по умолчанию, данные лежат в postgres. */
    V2("apiBaseUrl.v2", "/api/v1", "dbUrl.v2");

    /** Ключ в конфиге, где лежит адрес версии (хост + порт), например {@code apiBaseUrl.v2}. */
    private final String baseUrlKey;
    private final String basePath;

    /**
     * Ключ в конфиге, где лежит JDBC-адрес хранилища версии (например {@code dbUrl.v2}),
     * либо null, если внешнего хранилища у версии нет.
     */
    private final String dbUrlKey;

    /** Полный базовый URI для RestAssured: адрес из конфига + префикс пути. */
    public String baseUri() {
        return requiredProperty(baseUrlKey).replaceAll("/+$", "") + basePath;
    }

    /**
     * JDBC-адрес хранилища этой версии из конфига.
     * Вызывать только при {@link #hasDatabase()} — проверку делает {@link Config#getDbUrl()}.
     */
    public String getDbUrl() {
        return requiredProperty(dbUrlKey);
    }

    /**
     * Есть ли у версии внешнее хранилище, к которому можно ходить из тестов.
     * <p>
     * Это свойство версии, а не настройка: у легаси базы нет физически, и никакой
     * конфигурацией её не появится.
     */
    public boolean hasDatabase() {
        return dbUrlKey != null;
    }

    private String requiredProperty(String key) {
        String value = Config.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Не задана настройка версии " + this + ": ключ '" + key + "' в config.properties, "
                            + "-D" + key + " или переменная окружения "
                            + key.toUpperCase().replace('.', '_'));
        }
        return value;
    }
}
