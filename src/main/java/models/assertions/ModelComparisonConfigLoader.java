package models.assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Читает src/main/resources/model-comparison.properties и превращает его в набор
 * {@link ModelComparisonRule}, доступных по паре (requestClass, responseClass).
 * <p>
 * Формат файла (одна пара моделей на строку):
 * <pre>RequestClass=ResponseClass:field1=field1Response,field2=field2Response</pre>
 * <p>
 * Чтобы добавить сравнение для новой пары моделей, достаточно дописать ещё одну
 * строку в файл — этот класс и {@link ModelComparator} трогать не нужно.
 * <p>
 */
public final class ModelComparisonConfigLoader {

    private static final String CONFIG_FILE = "model-comparison.properties";
    private static final Map<String, ModelComparisonRule> RULES = load();

    private ModelComparisonConfigLoader() {
    }

    public static ModelComparisonRule getRule(Class<?> requestClass, Class<?> responseClass) {
        String requestName = requestClass.getSimpleName();
        String responseName = responseClass.getSimpleName();
        ModelComparisonRule rule = RULES.get(key(requestName, responseName));

        if (rule == null) {
            throw new IllegalStateException(
                    "Не найдено правило сравнения для " + requestName + " -> " + responseName
                            + ". Добавьте строку в " + CONFIG_FILE + ", например:" + System.lineSeparator()
                            + requestName + "=" + responseName + ":field1=field1,field2=field2"
            );
        }
        return rule;
    }

    private static String key(String requestClassName, String responseClassName) {
        return requestClassName + "->" + responseClassName;
    }

    private static Map<String, ModelComparisonRule> load() {
        Map<String, ModelComparisonRule> rules = new HashMap<>();

        try (InputStream input = ModelComparisonConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new IllegalStateException(CONFIG_FILE + " не найден в resources");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String rawLine;
                int lineNumber = 0;

                while ((rawLine = reader.readLine()) != null) {
                    lineNumber++;
                    ModelComparisonRule rule = parseLine(rawLine, lineNumber);
                    if (rule != null) {
                        rules.put(key(rule.getRequestClassName(), rule.getResponseClassName()), rule);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать " + CONFIG_FILE, e);
        }

        return rules;
    }

    private static ModelComparisonRule parseLine(String rawLine, int lineNumber) {
        String line = rawLine.trim();

        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
            return null;
        }

        int classSeparator = line.indexOf('=');
        int fieldsSeparator = line.indexOf(':');

        if (classSeparator < 0 || fieldsSeparator < 0 || fieldsSeparator < classSeparator) {
            throw new IllegalArgumentException(malformedLineMessage(rawLine, lineNumber));
        }

        String requestClassName = line.substring(0, classSeparator).trim();
        String responseClassName = line.substring(classSeparator + 1, fieldsSeparator).trim();
        String fieldsPart = line.substring(fieldsSeparator + 1).trim();

        if (requestClassName.isEmpty() || responseClassName.isEmpty() || fieldsPart.isEmpty()) {
            throw new IllegalArgumentException(malformedLineMessage(rawLine, lineNumber));
        }

        Map<String, String> fieldMapping = new LinkedHashMap<>();
        for (String pair : fieldsPart.split(",")) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                continue;
            }

            String[] parts = trimmedPair.split("=", 2);
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Некорректная пара полей \"" + trimmedPair + "\" на строке " + lineNumber
                                + " в " + CONFIG_FILE
                );
            }

            fieldMapping.put(parts[0].trim(), parts[1].trim());
        }

        if (fieldMapping.isEmpty()) {
            throw new IllegalArgumentException(malformedLineMessage(rawLine, lineNumber));
        }

        return new ModelComparisonRule(requestClassName, responseClassName, fieldMapping);
    }

    private static String malformedLineMessage(String rawLine, int lineNumber) {
        return "Некорректная строка " + lineNumber + " в " + CONFIG_FILE + ": \"" + rawLine + "\". "
                + "Ожидается формат: RequestClass=ResponseClass:field1=field1Response,field2=field2Response";
    }
}
