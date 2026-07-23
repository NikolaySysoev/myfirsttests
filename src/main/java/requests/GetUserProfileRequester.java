package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.requests.GetUserProfile;

import static io.restassured.RestAssured.given;

public class GetUserProfileRequester extends Request<GetUserProfile> {
    public GetUserProfileRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(GetUserProfile model) {
        return null;
        //TODO
    }

    @Override
    public ValidatableResponse put(GetUserProfile model) {
        return null;
        //TODO
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get("/api/v1/customer/profile")
                .then()
                .spec(responseSpecification);
    }
}
