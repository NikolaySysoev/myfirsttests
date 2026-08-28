package api.dao.comparison;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class DaoComparator {

    private final DaoComparisonConfigLoader configLoader;

    public DaoComparator() {
        this.configLoader = new DaoComparisonConfigLoader("dao-comparison.properties");
    }

    public void compare(Object apiResponse, Object dao) {
        DaoComparisonConfigLoader.DaoComparisonRule rule = configLoader.getRuleFor(apiResponse.getClass());

        if (rule == null) {
            throw new RuntimeException("No comparison rule found for " + apiResponse.getClass().getSimpleName());
        }

        Map<String, String> fieldMappings = rule.getFieldMappings();

        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String apiFieldName = mapping.getKey();
            String daoFieldName = mapping.getValue();

            Object apiValue = getFieldValue(apiResponse, apiFieldName);
            Object daoValue = getFieldValue(dao, daoFieldName);

            if (!valuesEqual(apiValue, daoValue)) {
                throw new AssertionError(String.format(
                        "Field mismatch for %s: API=%s, DAO=%s",
                        apiFieldName, apiValue, daoValue));
            }
        }
    }

    /**
     * Сравнивает значения полей. Числовые типы (например, BigDecimal у доменной
     * модели и Double у DAO) сравниваются по значению через BigDecimal, а не через
     * equals() — иначе 100 и 100.0 или BigDecimal("100.00") и Double 100.0 считались
     * бы разными только из-за разных классов/scale.
     */
    private boolean valuesEqual(Object apiValue, Object daoValue) {
        if (apiValue == daoValue) {
            return true;
        }
        if (apiValue == null || daoValue == null) {
            return false;
        }
        if (apiValue instanceof Number && daoValue instanceof Number) {
            return toBigDecimal((Number) apiValue).compareTo(toBigDecimal((Number) daoValue)) == 0;
        }
        return apiValue.equals(daoValue);
    }

    private BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        // BigDecimal.valueOf(double) идёт через Double.toString(), поэтому не тянет
        // артефакты двоичного округления, которые даёт конструктор new BigDecimal(double)
        return BigDecimal.valueOf(number.doubleValue());
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }
}
