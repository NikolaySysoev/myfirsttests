package models.assertions;

import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Кастомный AssertJ-ассёршн для сверки полей request-модели и response-модели.
 * <p>
 * Использование в тесте:
 * <pre>
 *     ModelAssertions.assertThatModels(createUserRequest, createUserResponse).match();
 * </pre>
 * <p>
 * Какие поля с какими сравнивать — не знает сам класс, это берётся из
 * model-comparison.properties через {@link ModelComparisonConfigLoader} /
 * {@link ModelComparator}. Поэтому новая пара моделей подключается без правок
 * в этом файле — достаточно новой строки в конфиге.
 * <p>
 * Тип actual/response намеренно {@code Object}, а не {@code BaseModel} — например
 * {@code CustomerResponse} не наследует {@code BaseModel}, и сужение типа отрезало бы
 * такие модели от сравнения.
 */
public class ModelAssertions extends AbstractAssert<ModelAssertions, Object> {

    private final Object response;

    private ModelAssertions(Object request, Object response) {
        super(request, ModelAssertions.class);
        this.response = response;
    }

    public static ModelAssertions assertThatModels(Object request, Object response) {
        return new ModelAssertions(request, response);
    }

    public ModelAssertions match() {
        isNotNull();

        if (response == null) {
            failWithMessage("Response модель должна быть не null");
            return this;
        }

        List<FieldMismatch> mismatches = ModelComparator.compare(actual, response);

        if (!mismatches.isEmpty()) {
            String details = mismatches.stream()
                    .map(FieldMismatch::toString)
                    .collect(Collectors.joining(System.lineSeparator()));

            failWithMessage(
                    "Ожидалось совпадение полей %s и %s, но найдено несовпадений: %d%n%s",
                    actual.getClass().getSimpleName(),
                    response.getClass().getSimpleName(),
                    mismatches.size(),
                    details
            );
        }

        return this;
    }
}
