package common.versioning;

import api.configs.BackendVersion;
import api.dao.checks.DbChecks;
import api.models.factory.DtoFactory;

/**
 * Единственная точка правды о том, на какой версии бэкенда выполняется
 * текущий тест.
 * <p>
 * Заполняется {@code ApiVersionExtension} в beforeEach и очищается в afterEach.
 * Всё, что зависит от версии в рантайме ({@code RequestSpecs}, фабрика DTO,
 * тексты ошибок), спрашивает версию здесь, а не читает системные свойства
 * самостоятельно.
 * <p>
 * Хранение — {@link ThreadLocal}, потому что junit-platform.properties уже
 * сконфигурирован под параллельный прогон.
 */
public final class ApiVersionContext {

    private static final ThreadLocal<BackendVersion> CURRENT = new ThreadLocal<>();

    private ApiVersionContext() {
    }

    public static void set(BackendVersion version) {
        CURRENT.set(version);
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Версия текущего теста.
     *
     * @throws IllegalStateException если версия не установлена — обычно это значит,
     *                               что {@code ApiVersionExtension} не зарегистрирован
     *                               в базовом классе теста
     */
    public static BackendVersion current() {
        BackendVersion version = CURRENT.get();
        if (version == null) {
            throw new IllegalStateException(
                    "Версия API не определена. Убедитесь, что ApiVersionExtension "
                            + "зарегистрирован в BaseApiTest и стоит раньше остальных экстеншенов.");
        }
        return version;
    }

    /** Фабрика DTO, соответствующая версии текущего теста. */
    public static DtoFactory dto() {
        return DtoFactory.of(current());
    }

    /**
     * Гейт проверок на уровне БД для версии текущего теста.
     * У версии без хранилища проверки пропускаются с отметкой в отчёте.
     */
    public static DbChecks db() {
        return DbChecks.of(current());
    }
}
