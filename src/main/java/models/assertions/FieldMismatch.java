package models.assertions;

import lombok.Getter;

/**
 * Одно несовпадение между полем request-модели и соответствующим полем response-модели.
 */
@Getter
public class FieldMismatch {

    private final String requestField;
    private final String responseField;
    private final Object requestValue;
    private final Object responseValue;

    public FieldMismatch(String requestField, String responseField, Object requestValue, Object responseValue) {
        this.requestField = requestField;
        this.responseField = responseField;
        this.requestValue = requestValue;
        this.responseValue = responseValue;
    }

    @Override
    public String toString() {
        return String.format(
                "  request.%s (%s) != response.%s (%s)",
                requestField, requestValue, responseField, responseValue
        );
    }
}
