package api.configs;

import common.versioning.ApiVersionContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Config INSTANSE = new Config();
    private final Properties properties = new Properties();

    private Config(){
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream("config.properties")){
            if (input == null) {
                throw new RuntimeException("config.properties no found in resources");
            }
            properties.load(input);
        } catch (IOException e){
            throw  new RuntimeException("Fail to load config.properties", e);
        }
    }

    /**
     * Значение настройки с учётом приоритетов:
     * <ol>
     *     <li>системное свойство JVM: {@code -DapiBaseUrl.v2=...}</li>
     *     <li>переменная окружения: ключ в верхнем регистре, точки заменены на {@code _}
     *         ({@code apiBaseUrl.v2 -> APIBASEURL_V2}, {@code db.username -> DB_USERNAME})</li>
     *     <li>{@code config.properties}</li>
     * </ol>
     */
    public static String getProperty(String key){
        // ПРИОРИТЕТ 1 - системное свойство: -DapiBaseUrl.v2=...
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }

        // ПРИОРИТЕТ 2 - переменная окружения: apiBaseUrl.v2 -> APIBASEURL_V2
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            return envValue;
        }

        // ПРИОРИТЕТ 3 - config.properties
        return INSTANSE.properties.getProperty(key);
    }

    /**
     * Версия бэкенда для текущего теста. Устанавливается {@code ApiVersionExtension},
     * см. {@link ApiVersionContext}.
     */
    public static BackendVersion getBackendVersion() {
        return ApiVersionContext.current();
    }

    /**
     * Базовый URI активной версии: адрес (хост + порт) из конфига и префикс пути из {@link BackendVersion}.
     */
    public static String getApiBaseUrl() {
        return getBackendVersion().baseUri();
    }

    /**
     * JDBC-адрес хранилища активной версии.
     *
     * @throws IllegalStateException если у версии внешнего хранилища нет — обращаться
     *                               к БД в этом случае нельзя, и молчаливый null здесь
     *                               был бы хуже явной ошибки
     */
    public static String getDbUrl() {
        BackendVersion version = getBackendVersion();
        if (!version.hasDatabase()) {
            throw new IllegalStateException(
                    "У версии " + version + " нет внешнего хранилища — запрос в БД невозможен. "
                            + "Проверки БД должны идти через DbChecks, он такие версии пропускает.");
        }
        return version.getDbUrl();
    }
}
