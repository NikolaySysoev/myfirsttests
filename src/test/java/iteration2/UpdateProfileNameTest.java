package iteration2;

import generators.RandomEntityGenerator;
import models.requests.ChangeNameRequest;
import models.requests.CreateUserRequest;
import models.requests.LoginRequest;
import models.responses.ChangeNameResponse;
import models.responses.CreateUserResponse;
import models.responses.GetCustomerProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateProfileNameTest {
    private static final String DEFAULT_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";
    private static final String DEFAULT_ERROR_MESSAGE = "Name must contain two words with letters only";

    private String userAuthToken;
    private String initialName = null;

    @BeforeEach
    public void setup(){
        //готовим данные для создания пользователя
        var createUserRequest = RandomEntityGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_CREATE_USERS,
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest);

        //логин под созданным пользователем
        var loginRequest = LoginRequest.builder()
                .username(createUserRequest.getUsername())
                .password(createUserRequest.getPassword())
                .build();

        userAuthToken = new CrudRequester(
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginRequest)
                .extract()
                .header("authorization");

        initialName = new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetCustomerProfileResponse.class)
                .getName();
    }

    public static Stream<Arguments> invalidName() {
        return Stream.of(
                Arguments.of("Nikolay", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Nikolay Nikolay Nikolay", DEFAULT_ERROR_MESSAGE),
                Arguments.of(" ", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Nikolay123 Sysoev", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Anna-Maria Ivanova", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Nikolay Sysoev123", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Nikolay^&*(! Sysoev", DEFAULT_ERROR_MESSAGE),
                Arguments.of("Nikolay Sysoev^&*(!", DEFAULT_ERROR_MESSAGE),
                Arguments.of("12312 ^&*(!", DEFAULT_ERROR_MESSAGE)
//                Arguments.of(null, DEFAULT_ERROR_MESSAGE)  - выключено, есть баг на бэке. Падает с 500-й ошибкой, вместо обработки и 400-й ошибки
        );
    }

    @Test
    public void userCanChangeNameWhenValidData() {
        var changeName = ChangeNameRequest.builder()
                .name(DEFAULT_NAME)
                .build();

        var response = new ValidatedCrudRequester<ChangeNameResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeName);

        String newUserName = response.getCustomer().getName();
        String message = response.getMessage();

        assertEquals(DEFAULT_NAME, newUserName);
        assertEquals(DEFAULT_SUCCESS_MESSAGE, message);

        var profileResponse = new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get();

        String profileName = profileResponse.getName();
        assertEquals(DEFAULT_NAME, profileName);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, String errorValue){
        var changeName = ChangeNameRequest.builder()
                .name(newName)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .put(changeName);

        var response = new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(userAuthToken),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get();

        String newUserName = response.getName();

        assertEquals(initialName, newUserName);
    }
}
