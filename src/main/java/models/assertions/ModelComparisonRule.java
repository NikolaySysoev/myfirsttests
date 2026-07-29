package models.assertions;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Одно правило сравнения, разобранное из строки model-comparison.properties:
 * <pre>RequestClass=ResponseClass:field1=field1Response,field2=field2Response</pre>
 * <p>
 * Хранит имена классов (simple name) и маппинг "поле запроса -> поле ответа".
 */
@Getter
public class ModelComparisonRule {

    private final String requestClassName;
    private final String responseClassName;
    private final Map<String, String> fieldMapping;

    public ModelComparisonRule(String requestClassName, String responseClassName, Map<String, String> fieldMapping) {
        this.requestClassName = requestClassName;
        this.responseClassName = responseClassName;
        this.fieldMapping = Collections.unmodifiableMap(new LinkedHashMap<>(fieldMapping));
    }
}
