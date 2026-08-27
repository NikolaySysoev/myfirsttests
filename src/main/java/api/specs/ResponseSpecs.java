package api.specs;

import api.models.domain.ApiError;
import api.models.domain.ExpectedError;
import common.versioning.ApiVersionContext;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    private ResponseSpecs() {
    }

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK() {
        return defaultResponseBuilder().
                expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequest(ApiError error) {
        return expectError(
                defaultResponseBuilder().expectStatusCode(HttpStatus.SC_BAD_REQUEST),
                error)
                .build();
    }

    public static ResponseSpecification requestReturnsForbidden(ApiError error) {
        return expectError(
                defaultResponseBuilder().expectStatusCode(HttpStatus.SC_FORBIDDEN),
                error)
                .build();
    }

    /**
     * Ожидание ошибки с учётом того, как активная версия её оформляет.
     * <p>
     * Ни текста, ни пути здесь нет: и то и другое отдаёт фабрика версии, поэтому
     * switch по версиям остаётся ровно один — в {@code DtoFactory.of()}.
     */
    private static ResponseSpecBuilder expectError(ResponseSpecBuilder builder, ApiError error) {
        ExpectedError expected = ApiVersionContext.dto().expect(error);

        return expected.getJsonPath() == null
                ? builder.expectBody(Matchers.equalTo(expected.getMessage()))
                : builder.expectBody(expected.getJsonPath(), Matchers.equalTo(expected.getMessage()));
    }
}
