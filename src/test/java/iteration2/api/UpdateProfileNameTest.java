package iteration2.api;

import iteration2.BaseTest;
import api.models.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.requests.ChangeNameRequest;
import api.models.responses.ChangeNameResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateProfileNameTest extends BaseTest {
    private static final String DEFAULT_VALID_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";

    private String initialName = null;
    private String username;
    private String password;

    @BeforeEach
    public void setup(){
        //создаем пользователя
        var createUserRequest = AdminSteps.createUser();

        username = createUserRequest.getUsername();
        password = createUserRequest.getPassword();

        //вытаскиваем имя по умолчанию, заданное после создания пользователя
        initialName = UserSteps.getCustomerProfile(username, password).getName();
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

    @Test
    public void userCanChangeNameWhenValidData() {
        var changeNameRequest = ChangeNameRequest.builder()
                .name(DEFAULT_VALID_NAME)
                .build();

        var changeNameResponse = new ValidatedCrudRequester<ChangeNameResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeNameRequest);

        ModelAssertions.assertThatModels(changeNameRequest,changeNameResponse).match();


        String newUserName = changeNameResponse.getCustomer().getName();
        String message = changeNameResponse.getMessage();

        softly.assertThat(newUserName).isEqualTo(DEFAULT_VALID_NAME);
        softly.assertThat(message).isEqualTo(DEFAULT_SUCCESS_MESSAGE);

        var profileResponse = UserSteps.getCustomerProfile(username, password);
        String profileName = profileResponse.getName();

        assertEquals(DEFAULT_VALID_NAME, profileName);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, String errorValue) {
        var changeName = ChangeNameRequest.builder()
                .name(newName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .put(changeName);

        var response = UserSteps.getCustomerProfile(username, password);

        String newUserName = response.getName();

        assertEquals(initialName, newUserName);
    }
}
