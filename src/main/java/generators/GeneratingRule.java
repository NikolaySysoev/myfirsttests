package generators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Указывает, что значение поля должно генерироваться строго по регулярному выражению,
 * а не по стандартному правилу для его типа.
 *
 * Пример:
 * <pre>
 *   {@literal @}GeneratingRule("\\+7[0-9]{10}")
 *   private String phone;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratingRule {

    /** Регулярное выражение, которому должно соответствовать сгенерированное значение. */
    String value();
}