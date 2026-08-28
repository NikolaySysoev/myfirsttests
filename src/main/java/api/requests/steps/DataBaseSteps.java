package api.requests.steps;

import api.configs.Config;
import api.dao.AccountDao;
import api.dao.UserDao;
import api.database.Condition;
import api.database.DBRequest;
import common.helpers.StepLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Обращения к базе данных.
 * <p>
 * Основа — один универсальный {@link #findOne}: он не знает ни про конкретные
 * таблицы, ни про конкретные DAO, поэтому новая модель не требует здесь нового
 * метода. Именованные методы ниже — тонкие обёртки ради читаемости в тестах,
 * заводить их обязательно не нужно.
 * <p>
 * ВАЖНО: эти методы предполагают, что у активной версии бэкенда есть внешнее
 * хранилище. Вызывать их напрямую из теста не следует — проверки должны идти
 * через {@code DbChecks}, который на версии без БД просто их не выполняет.
 */
public class DataBaseSteps {

    /**
     * Одна запись из таблицы по равенству колонки значению.
     *
     * @param table    имя таблицы
     * @param column   колонка условия
     * @param value    значение условия
     * @param daoClass класс, в который разложить строку результата
     * @return найденная запись либо null, если строк нет
     */
    public static <T> T findOne(String table, String column, Object value, Class<T> daoClass) {
        return StepLogger.log(
                "Запрос в БД: " + table + " где " + column + " = " + value,
                () -> {
                    return DBRequest.builder()
                            .requestType(DBRequest.RequestType.SELECT)
                            .table(table)
                            .where(Condition.equalTo(column, value))
                            .extractAs(daoClass);
                });
    }

    // ---------- именованные обёртки ----------

    public static UserDao getUserByUsername(String username) {
        return findOne("customers", "username", username, UserDao.class);
    }

    public static UserDao getUserById(Long id) {
        return findOne("customers", "id", id, UserDao.class);
    }

    public static UserDao getUserByRole(String role) {
        return findOne("customers", "role", role, UserDao.class);
    }

    public static AccountDao getAccountByAccountNumber(String accountNumber) {
        return findOne("accounts", "account_number", accountNumber, AccountDao.class);
    }

    public static AccountDao getAccountById(Long id) {
        return findOne("accounts", "id", id, AccountDao.class);
    }

    /** Раньше искал в таблице customers — это была опечатка, счета лежат в accounts. */
    public static AccountDao getAccountByCustomerId(Long customerId) {
        return findOne("accounts", "customer_id", customerId, AccountDao.class);
    }

    // ---------- запись ----------

    public static void updateAccountBalance(Long accountId, Double newBalance) {
        StepLogger.log("Update account balance in database for account ID: " + accountId + " to: " + newBalance, () -> {
            try (Connection connection = DriverManager.getConnection(
                    Config.getDbUrl(),
                    Config.getProperty("db.username"),
                    Config.getProperty("db.password"))) {

                String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setDouble(1, newBalance);
                    statement.setLong(2, accountId);
                    int rowsAffected = statement.executeUpdate();

                    if (rowsAffected == 0) {
                        throw new RuntimeException("No account found with ID: " + accountId);
                    }

                    return rowsAffected;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update account balance", e);
            }
        });
    }
}
