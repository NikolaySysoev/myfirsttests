package requests.steps;

import generators.RandomEntityGenerator;
import models.requests.CreateUserRequest;
import models.responses.CreateUserResponse;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

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
