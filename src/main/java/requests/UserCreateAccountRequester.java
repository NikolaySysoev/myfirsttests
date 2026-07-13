package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateAccountUserRequest;

import static io.restassured.RestAssured.given;

public class UserCreateAccountRequester extends Request<CreateAccountUserRequest> {

    public UserCreateAccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(CreateAccountUserRequest model) {
        return given().
                spec(requestSpecification)
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
