package iteration2;

import models.requests.ChangeNameRequest;
import models.responses.ChangeNameResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateProfileNameTest extends BaseTest {
    private static final String DEFAULT_VALID_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";
    private static final String DEFAULT_ERROR_MESSAGE = "Name must contain two words with letters only";

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
                .name(DEFAULT_VALID_NAME)
                .build();

        var response = new ValidatedCrudRequester<ChangeNameResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeName);

        String newUserName = response.getCustomer().getName();
        String message = response.getMessage();

        softly.assertThat(newUserName).isEqualTo(DEFAULT_VALID_NAME);
        softly.assertThat(message).isEqualTo(DEFAULT_SUCCESS_MESSAGE);

        var profileResponse = UserSteps.getCustomerProfile(username, password);
        String profileName = profileResponse.getName();

        assertEquals(DEFAULT_VALID_NAME, profileName);
    }

    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, String errorValue){
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
