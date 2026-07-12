package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.DepositMoneyUserRequest;

import static io.restassured.RestAssured.given;


public class UserDepositMoneyRequester extends  Request<DepositMoneyUserRequest> {


    public UserDepositMoneyRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(DepositMoneyUserRequest model) {
        return  given()
                .spec(requestSpecification)
                .body(model)
                .post("/api/v1/accounts/deposit")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return null;
        //TODO
    }
}
