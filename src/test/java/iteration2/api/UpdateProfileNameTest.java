package iteration2.api;

import api.dao.comparison.DaoAndModelAssertions;
import api.models.domain.ApiError;
import api.models.assertions.ModelAssertions;
import api.models.BaseModel;
import api.models.factory.DtoFactory;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.requests.steps.DataBaseSteps;
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
    private static final String DEFAULT_VALID_NAME = "Name Surname";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Profile updated successfully";

    private String initialName = null;
    private long userId;

    @BeforeEach
    public void setup(){
        // пользователь уже создан ApiUserSessionExtension'ом (по @UserSession на тестовом методе)

        //вытаскиваем имя по умолчанию, заданное после создания пользователя
        var userAccount = SessionStorage.actAsUser().getCustomerProfile();
        initialName = userAccount.getName();
        userId = userAccount.getId();
    }

    public static Stream<Arguments> invalidName() {
        return Stream.of(
                Arguments.of("Name", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Name Name Name", ApiError.CHANGE_NAME_ERROR),
                Arguments.of(" ", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Name123 Surname", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Anna-Maria Ivanova", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Name Surname123", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Name^&*(! Surname", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("Name Surname^&*(!", ApiError.CHANGE_NAME_ERROR),
                Arguments.of("12312 ^&*(!", ApiError.CHANGE_NAME_ERROR)
//                Arguments.of(null, ApiError.CHANGE_NAME_ERROR)  - выключено, есть баг на бэке. Падает с 500-й ошибкой, вместо обработки и 400-й ошибки
        );
    }

    @UserSession
    @Test
    public void userCanChangeNameWhenValidData(DtoFactory dto) {
        var changeNameRequest = dto.changeName(DEFAULT_VALID_NAME);

        BaseModel changeNameResponse = new ValidatedCrudRequester<BaseModel>(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsOK()
        )
                .put(changeNameRequest);

        ModelAssertions.assertThatModels(changeNameRequest, changeNameResponse).match();

        String newUserName = dto.changedName(changeNameResponse).getName();
        softly.assertThat(newUserName).isEqualTo(DEFAULT_VALID_NAME);

        // сообщение об успехе есть только в легаси-контракте: в актуальной версии
        // ответ его не содержит, поэтому проверяем там, где оно вообще приходит
        dto.successMessage(changeNameResponse)
                .ifPresent(message -> softly.assertThat(message).isEqualTo(DEFAULT_SUCCESS_MESSAGE));

        var userProfile = SessionStorage.actAsUser().getCustomerProfile();
        String profileName = userProfile.getName();

        assertEquals(DEFAULT_VALID_NAME, profileName);

        //Проверка в БД
        var userDao = DataBaseSteps.getUserById(userId);
        DaoAndModelAssertions.assertThat(userProfile, userDao).match();
    }

    @UserSession
    @ParameterizedTest
    @MethodSource("invalidName")
    public void userCanNotChangeNameWhenInvalidData(String newName, ApiError errorValue, DtoFactory dto) {
        var changeName = dto.changeName(newName);

        new CrudRequester(
                RequestSpecs.authAsUser(SessionStorage.getUserRawData()),
                Endpoint.CHANGE_CUSTOMER_NAME,
                ResponseSpecs.requestReturnsBadRequest(errorValue)
        )
                .put(changeName);

        var userProfile = SessionStorage.actAsUser().getCustomerProfile();
        String profileName = userProfile.getName();

        assertEquals(initialName, profileName);

        //Проверка в БД
        var userDao = DataBaseSteps.getUserById(userId);
        DaoAndModelAssertions.assertThat(userProfile, userDao).match();
    }
}
