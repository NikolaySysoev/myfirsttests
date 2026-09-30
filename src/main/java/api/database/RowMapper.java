package api.database;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Превращает строку {@link ResultSet} в объект DAO по именам полей.
 * <p>
 * Один маппер на все DAO: раньше в {@code DBRequest} была цепочка
 * {@code if (clazz == UserDao.class) ...} и рукописный метод-маппер на каждый
 * класс — то есть стоимость росла линейно с числом моделей. Здесь соответствие
 * ищется по имени: имя колонки и имя поля приводятся к общему виду (нижний
 * регистр без подчёркиваний), поэтому {@code account_number} находит поле
 * {@code accountNumber}, а {@code id} — {@code id}.
 * <p>
 * Новый DAO не требует здесь ни строки — достаточно, чтобы имена полей
 * соответствовали колонкам.
 */
public final class RowMapper {

    private RowMapper() {
    }

    /**
     * Собирает объект указанного класса из текущей строки результата.
     *
     * @throws IllegalStateException если у класса нет конструктора без аргументов
     *                               или тип колонки не приводится к типу поля
     */
    public static <T> T map(ResultSet resultSet, Class<T> clazz) throws SQLException {
        Map<String, Object> row = readRow(resultSet);

        T instance;
        try {
            instance = clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "У " + clazz.getSimpleName() + " нет конструктора без аргументов. "
                            + "Для DAO это обязательно — добавьте @NoArgsConstructor.", e);
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            Object value = row.get(normalize(field.getName()));
            if (value == null) {
                continue;
            }
            field.setAccessible(true);
            try {
                field.set(instance, convert(value, field.getType(), clazz, field));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "Не удалось записать поле " + clazz.getSimpleName() + "." + field.getName(), e);
            }
        }

        return instance;
    }

    /** Колонки текущей строки, ключи приведены к сравнимому виду. */
    private static Map<String, Object> readRow(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String column = meta.getColumnLabel(i);
            row.put(normalize(column), resultSet.getObject(i));
        }
        return row;
    }

    /** account_number, accountNumber, ACCOUNT_NUMBER -> accountnumber */
    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase();
    }

    /**
     * Приводит значение из базы к типу поля DAO. Драйвер отдаёт, например,
     * {@code BigDecimal} для numeric и {@code Long} для bigint — а в DAO поле может
     * быть объявлено как {@code Double} или {@code Long}.
     */
    private static Object convert(Object value, Class<?> target, Class<?> daoClass, Field field) {
        if (target.isInstance(value)) {
            return value;
        }
        if (value instanceof Number number) {
            if (target == Long.class || target == long.class) return number.longValue();
            if (target == Integer.class || target == int.class) return number.intValue();
            if (target == Double.class || target == double.class) return number.doubleValue();
            if (target == Float.class || target == float.class) return number.floatValue();
            if (target == BigDecimal.class) return new BigDecimal(number.toString());
            if (target == BigInteger.class) return new BigInteger(number.toString());
        }
        if (value instanceof Timestamp timestamp) {
            if (target == LocalDateTime.class) return timestamp.toLocalDateTime();
            if (target == Date.class) return new Date(timestamp.getTime());
        }
        if (target == String.class) {
            return value.toString();
        }
        if (target == Boolean.class || target == boolean.class) {
            if (value instanceof Number number) return number.intValue() != 0;
            return Boolean.parseBoolean(value.toString());
        }
        throw new IllegalStateException(
                "Не удалось привести значение типа " + value.getClass().getSimpleName()
                        + " к типу поля " + daoClass.getSimpleName() + "." + field.getName()
                        + " (" + target.getSimpleName() + "). Добавьте правило в RowMapper.convert().");
    }
}
