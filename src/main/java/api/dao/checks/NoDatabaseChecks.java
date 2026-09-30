package api.dao.checks;

import api.configs.BackendVersion;
import common.helpers.StepLogger;

import java.util.function.Supplier;

/**
 * Версия без внешнего хранилища: сверять не с чем.
 * <p>
 * Пропуск намеренно шумный — в отчёт пишется отдельный шаг. Зелёный тест, который
 * молча не выполнил проверку, хуже красного: по отчёту было бы не отличить
 * «сверили с базой» от «сверять было негде».
 */
public class NoDatabaseChecks implements DbChecks {

    private final BackendVersion version;

    public NoDatabaseChecks(BackendVersion version) {
        this.version = version;
    }

    @Override
    public void assertMatches(Object apiModel, Supplier<Object> daoSupplier) {
        // daoSupplier намеренно не вызывается: соединение не открывается
        StepLogger.log(
                "Проверка в БД пропущена: у версии " + version + " нет внешнего хранилища",
                () -> {
                });
    }
}
