package api.requests.steps;

import api.generators.RandomEntityGenerator;
import api.models.requests.CreateUserRequest;
import api.models.responses.CreateUserResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

public class AdminSteps {
    public static CreateUserRequest createUser(){
        //готовим данные для создания пользователя
        var createUserRequest = RandomEntityGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_CREATE_USERS,
                ResponseSpecs.entityWasCreated()
        )
                .post(createUserRequest);

        //возвращает данные для запроса, чтобы вытащить логин и пароль до хэширования
        return createUserRequest;
    }
}
