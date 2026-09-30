package api.dao.checks;

import api.dao.comparison.DaoAndModelAssertions;

import java.util.function.Supplier;

/**
 * Версия с внешним хранилищем: сверка выполняется по-настоящему.
 * <p>
 * Какие поля сравнивать, решает {@code dao-comparison.properties} — поэтому новая
 * пара моделей не требует здесь ни строки.
 */
public class DatabaseChecks implements DbChecks {

    @Override
    public void assertMatches(Object apiModel, Supplier<Object> daoSupplier) {
        DaoAndModelAssertions.assertThat(apiModel, daoSupplier.get()).match();
    }
}
