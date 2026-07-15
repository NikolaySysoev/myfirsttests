package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.requests.ChangeNameRequest;

import static io.restassured.RestAssured.given;

public class ChangeNameRequester extends Request<ChangeNameRequest> {
    public ChangeNameRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(ChangeNameRequest model) {
        return null;
        //TODO
    }

    @Override
    public ValidatableResponse put(ChangeNameRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put("/api/v1/customer/profile")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return null;
        //TODO
    }
}
