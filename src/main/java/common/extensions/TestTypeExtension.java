package common.extensions;

import api.configs.Config;
import common.annotations.TestType;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

public class TestTypeExtension implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
        TestType annotation = extensionContext.getElement()
                .map(el -> el.getAnnotation(TestType.class))
                .orElse(null);

        if (annotation == null) {
            return ConditionEvaluationResult.enabled("Тест не размечен -> попадает в прогон");
        }

        String currentTestScope = Config.getProperty("TestScope"); //smoke or regress
        boolean matches = Arrays.stream(annotation.value())
                .anyMatch(annotationTestType -> annotationTestType.equalsIgnoreCase(currentTestScope));

        if (matches) {
            return ConditionEvaluationResult.enabled("Тест размечен как " + Arrays.toString(annotation.value()) + " -> попадает в прогон");
        } {
            return ConditionEvaluationResult.disabled("Тест размечен как " + Arrays.toString(annotation.value()) +
                    " а текущий скоуп = [" + currentTestScope + "] -> не попадает в прогон");
        }
    }
}
