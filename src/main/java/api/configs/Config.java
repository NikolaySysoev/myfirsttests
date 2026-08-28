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

    public static String getProperty(String key){
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
     * Базовый URI активной версии: хост, порт и префикс пути.
     * Порт и префикс заданы в {@link BackendVersion}, а не в config.properties.
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
