package generators;

import com.mifmif.common.regex.Generex;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Генерирует объект произвольного data class'а, заполняя все поля случайными значениями,
 * подобранными по типу поля.
 *
 * Если поле помечено {@link GeneratingRule}, значение генерируется строго по regex из аннотации
 * (через Generex), независимо от типа поля (регэксп должен соответствовать String-полю).
 */
public class RandomEntityGenerator {

    private static final int DEFAULT_STRING_LENGTH = 10;
    private static final int DEFAULT_COLLECTION_SIZE = 3;
    private static final String ALPHANUMERIC =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private RandomEntityGenerator() {
    }

    public static <T> T generate(Class<T> clazz) {
        try {
            T instance = instantiate(clazz);
            for (Field field : getAllFields(clazz)) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, generateValueForField(field));
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось сгенерировать сущность для класса " + clazz.getName(), e);
        }
    }

    private static <T> T instantiate(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /** Собирает поля класса вместе с полями всех родительских классов. */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Object generateValueForField(Field field) {
        GeneratingRule rule = field.getAnnotation(GeneratingRule.class);
        if (rule != null) {
            return new Generex(rule.value()).random();
        }
        return generateValueForType(field.getType(), field.getGenericType());
    }

    private static Object generateValueForType(Class<?> type, Type genericType) {
        var random = ThreadLocalRandom.current();

        if (type == String.class) {
            return randomString(DEFAULT_STRING_LENGTH);
        }
        if (type == int.class || type == Integer.class) {
            return random.nextInt(1, 10_000);
        }
        if (type == long.class || type == Long.class) {
            return random.nextLong(1, 1_000_000L);
        }
        if (type == double.class || type == Double.class) {
            return random.nextDouble(1, 10_000);
        }
        if (type == float.class || type == Float.class) {
            return (float) random.nextDouble(1, 10_000);
        }
        if (type == boolean.class || type == Boolean.class) {
            return random.nextBoolean();
        }
        if (type == BigDecimal.class) {
            return BigDecimal.valueOf(random.nextDouble(1, 10_000)).setScale(2, RoundingMode.HALF_UP);
        }
        if (type == UUID.class) {
            return UUID.randomUUID();
        }
        if (type == LocalDate.class) {
            return LocalDate.now().minusDays(random.nextInt(0, 365));
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.now().minusMinutes(random.nextInt(0, 100_000));
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants[random.nextInt(constants.length)];
        }
        if (List.class.isAssignableFrom(type)) {
            return generateList(genericType);
        }
        if (!type.isPrimitive() && !type.getName().startsWith("java.")) {
            // вложенный кастомный data class — генерируем рекурсивно
            return generate(type);
        }
        return null;
    }

    private static List<Object> generateList(Type genericType) {
        List<Object> list = new ArrayList<>();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type elementType = parameterizedType.getActualTypeArguments()[0];
            if (elementType instanceof Class<?> elementClass) {
                for (int i = 0; i < DEFAULT_COLLECTION_SIZE; i++) {
                    list.add(generateValueForType(elementClass, null));
                }
            }
        }
        return list;
    }

    private static String randomString(int length) {
        var random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}