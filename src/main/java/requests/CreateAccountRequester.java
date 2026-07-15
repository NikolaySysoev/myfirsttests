package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.requests.CreateAccountRequest;

import static io.restassured.RestAssured.given;

public class CreateAccountRequester extends Request<CreateAccountRequest> {

    public CreateAccountRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(CreateAccountRequest model) {
        return given().
                spec(requestSpecification)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse put(CreateAccountRequest model) {
        return null;
        //TODO
    }

    @Override
    public ValidatableResponse get() {
        return null;
        //TODO
    }
}
