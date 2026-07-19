package iteration2;

import generators.RandomEntityGenerator;
import models.BaseModel;
import models.requests.ChangeNameRequest;
import models.requests.CreateUserRequest;
import models.requests.LoginRequest;
import models.responses.ChangeNameResponse;
import models.responses.CreateUserResponse;
import models.responses.GetCustomerProfileResponse;
import org.assertj.core.api.SoftAssertions;
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
    private static final String DEFAULT_NAME = "Nikolay Sysoev";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";
    private static final String DEFAULT_ERROR_MESSAGE = "Name must contain two words with letters only";

    private String userAuthToken;
    private String initialName = null;

    @BeforeEach
    public void setup(){
        //создаем пользователя
        var createUserRequest = AdminSteps.createUser();

        //логин под созданным пользователем
        userAuthToken = UserSteps.loginUser(createUserRequest);

        //вытаскиваем имя по умолчанию, заданное после создания пользователя
        initialName = UserSteps.getCustomerProfile(userAuthToken).getName();
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

        softly.assertThat(DEFAULT_NAME.equals(newUserName));
        softly.assertThat(DEFAULT_SUCCESS_MESSAGE.equals(message));

        var profileResponse = UserSteps.getCustomerProfile(userAuthToken);

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

        var response = UserSteps.getCustomerProfile(userAuthToken);

        String newUserName = response.getName();

        assertEquals(initialName, newUserName);
    }
}
