package models.assertions;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сравнивает объект запроса и объект ответа по полям, описанным в
 * model-comparison.properties (правило достаётся через {@link ModelComparisonConfigLoader}).
 * <p>
 * Работает через reflection и не завязан на конкретные классы — любая новая пара
 * моделей поддерживается автоматически, как только для неё появится строка в конфиге.
 */
public final class ModelComparator {

    private ModelComparator() {
    }

    public static List<FieldMismatch> compare(Object request, Object response) {
        Objects.requireNonNull(request, "request не может быть null");
        Objects.requireNonNull(response, "response не может быть null");

        ModelComparisonRule rule = ModelComparisonConfigLoader.getRule(request.getClass(), response.getClass());

        List<FieldMismatch> mismatches = new ArrayList<>();

        for (Map.Entry<String, String> mapping : rule.getFieldMapping().entrySet()) {
            String requestFieldName = mapping.getKey();
            String responseFieldName = mapping.getValue();

            Object requestValue = readField(request, requestFieldName);
            Object responseValue = readField(response, responseFieldName);

            if (!valuesEqual(requestValue, responseValue)) {
                mismatches.add(new FieldMismatch(requestFieldName, responseFieldName, requestValue, responseValue));
            }
        }

        return mismatches;
    }

    /**
     * Сравнивает два значения поля. Для BigDecimal — через compareTo (игнорируя scale:
     * 10000 и 10000.0 — одно и то же число, а не разные объекты, как считает equals()).
     * Для всего остального — обычный Objects.equals.
     */
    private static boolean valuesEqual(Object requestValue, Object responseValue) {
        if (requestValue instanceof BigDecimal && responseValue instanceof BigDecimal) {
            return ((BigDecimal) requestValue).compareTo((BigDecimal) responseValue) == 0;
        }
        return Objects.equals(requestValue, responseValue);
    }

    /** Сегмент пути: имя поля, опционально с индексом в квадратных скобках, например accounts[0]. */
    private static final Pattern PATH_SEGMENT = Pattern.compile("^([A-Za-z_$][A-Za-z0-9_$]*)(\\[(\\d+)])?$");

    /**
     * Читает значение поля, поддерживая вложенность через точку ("customer.name")
     * и индексацию списков/массивов ("accounts[0].balance", "transactions[0].amount").
     * Если на каком-то шаге промежуточный объект окажется null — вернёт null,
     * не упав с NPE (это будет честно показано как несовпадение при сравнении).
     */
    private static Object readField(Object target, String fieldPath) {
        Object current = target;

        for (String segment : fieldPath.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = readSegment(current, segment, fieldPath);
        }

        return current;
    }

    /** Разбирает один сегмент пути (fieldName или fieldName[index]) и читает его из target. */
    private static Object readSegment(Object target, String segment, String fullPath) {
        Matcher matcher = PATH_SEGMENT.matcher(segment);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Некорректный сегмент \"" + segment + "\" в пути \"" + fullPath + "\". "
                            + "Ожидается имя поля, например customer или accounts[0]"
            );
        }

        String fieldName = matcher.group(1);
        String indexGroup = matcher.group(3);

        Object value = readSingleField(target, fieldName, fullPath);

        if (indexGroup == null) {
            return value;
        }
        if (value == null) {
            return null;
        }

        return readAtIndex(value, Integer.parseInt(indexGroup), segment, fullPath);
    }

    /** Достаёт элемент по индексу из List или обычного/примитивного массива. */
    private static Object readAtIndex(Object collectionOrArray, int index, String segment, String fullPath) {
        int size;

        if (collectionOrArray instanceof List) {
            size = ((List<?>) collectionOrArray).size();
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(indexOutOfBoundsMessage(segment, fullPath, index, size));
            }
            return ((List<?>) collectionOrArray).get(index);
        }

        if (collectionOrArray.getClass().isArray()) {
            size = Array.getLength(collectionOrArray);
            if (index < 0 || index >= size) {
                throw new IllegalArgumentException(indexOutOfBoundsMessage(segment, fullPath, index, size));
            }
            return Array.get(collectionOrArray, index);
        }

        throw new IllegalArgumentException(
                "Сегмент \"" + segment + "\" пути \"" + fullPath + "\" пытается индексировать "
                        + collectionOrArray.getClass().getSimpleName() + ", а это не List и не массив"
        );
    }

    private static String indexOutOfBoundsMessage(String segment, String fullPath, int index, int size) {
        return "Индекс " + index + " вне границ (размер " + size + ") в сегменте \"" + segment
                + "\" пути \"" + fullPath + "\"";
    }

    private static Object readSingleField(Object target, String fieldName, String fullPath) {
        Field field = findField(target.getClass(), fieldName);

        if (field == null) {
            throw new IllegalArgumentException(
                    "Поле \"" + fieldName + "\" (из пути \"" + fullPath + "\") не найдено в "
                            + target.getClass().getSimpleName()
                            + ". Проверьте название поля в model-comparison.properties"
            );
        }

        field.setAccessible(true);
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Не удалось прочитать поле \"" + fieldName + "\" из " + target.getClass().getSimpleName(), e
            );
        }
    }

    /** Ищет поле по имени в классе и во всех его родителях (кроме Object). */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
