package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateAccountUserRequest;

import static io.restassured.RestAssured.given;

public class UserCreateAccountRequester extends  Request<CreateAccountUserRequest>{

    public UserCreateAccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(CreateAccountUserRequest model) {
        RequestSpecification spec = given().spec(requestSpecification);

        if (model != null) {
            spec = spec.body(model);
        }

        return spec
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return null;
        //TODO
    }
}
