package iteration2.api;

import api.models.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.requests.ChangeNameRequest;
import api.models.responses.ChangeNameResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateProfileNameTest extends BaseApiTest {
    private static final String DEFAULT_VALID_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";

    private String initialName = null;

    @BeforeEach
    public void setup(){
        // пользователь уже создан ApiUserSessionExtension'ом (по @UserSession на тестовом методе)

        //вытаскиваем имя по умолчанию, заданное после создания пользователя
        initialName = SessionStorage.actAsUser().getCustomerProfile().getName();
    }

    public static Stream<Arguments> invalidName() {
        return Stream.of(
                Arguments.of("Nikolay", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Nikolay Nikolay Nikolay", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of(" ", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Nikolay123 Sysoev", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Anna-Maria Ivanova", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Nikolay Sysoev123", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Nikolay^&*(! Sysoev", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("Nikolay Sysoev^&*(!", ApiError.CHANGE_NAME_ERROR.getMessage()),
                Arguments.of("12312 ^&*(!", ApiError.CHANGE_NAME_ERROR.getMessage())
//                Arguments.of(null, ApiError.CHANGE_NAME_ERROR.getMessage())  - выключено, есть баг на бэке. Падает с 500-й ошибкой, вместо обработки и 400-й ошибки
        );
    }

    @UserSession
    @Test
    public void userCanChangeNameWhenValidData() {
        var changeNameRequest = ChangeNameRequest.builder()
                .name(DEFAULT_VALID_NAME)
                .build();

        var changeNameResponse = new ValidatedCrudRequester<ChangeNameResponse>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeNameRequest);

        ModelAssertions.assertThatModels(changeNameRequest,changeNameResponse).match();


        String newUserName = changeNameResponse.getCustomer().getName();
        String message = changeNameResponse.getMessage();

        softly.assertThat(newUserName).isEqualTo(DEFAULT_VALID_NAME);
        softly.assertThat(message).isEqualTo(DEFAULT_SUCCESS_MESSAGE);

        String profileName = SessionStorage.actAsUser().getCustomerProfile().getName();

        assertEquals(DEFAULT_VALID_NAME, profileName);
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, String errorValue) {
        var changeName = ChangeNameRequest.builder()
                .name(newName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .put(changeName);

        String newUserName = SessionStorage.actAsUser().getCustomerProfile().getName();

        assertEquals(initialName, newUserName);
    }
}
