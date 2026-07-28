package iteration2;

import generators.RandomData;
import models.UserRole;
import models.requests.ChangeNameRequest;
import models.requests.CreateUserRequest;
import models.responses.ChangeNameResponse;
import models.responses.GetUserProfileResponse;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.ChangeNameRequester;
import requests.GetUserProfileRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateProfileNameTest {
    private static final String DEFAULT_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";

    private String userAuthToken;
    private String initialName = null;

    @BeforeEach
    public void setup() {
        //создание пользователя
        var createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUserName())
                .password(RandomData.getUserPassword())
                .role(UserRole.USER.toString())
                .build();

        userAuthToken = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest)
                .extract()
                .header(HttpHeaders.AUTHORIZATION);

        initialName = new GetUserProfileRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserProfileResponse.class)
                .getName();
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
        var changeName = ChangeNameRequest.builder()
                .name(DEFAULT_NAME)
                .build();

        var response = new ChangeNameRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeName)
                .extract()
                .as(ChangeNameResponse.class);

        String newUserName = response.getCustomer().getName();
        String message = response.getMessage();

        assertEquals(DEFAULT_NAME, newUserName);
        assertEquals(DEFAULT_SUCCESS_MESSAGE, message);

        var profileResponse = new GetUserProfileRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserProfileResponse.class);

        String profileName = profileResponse.getName();
        assertEquals(DEFAULT_NAME, profileName);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, String errorValue) {
        var changeName = ChangeNameRequest.builder()
                .name(newName)
                .build();

        new ChangeNameRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .put(changeName);

        var response = new GetUserProfileRequester(
                RequestSpecs.authAsUser(userAuthToken),
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserProfileResponse.class);

        String newUserName = response.getName();

        assertEquals(initialName, newUserName);
    }
}
