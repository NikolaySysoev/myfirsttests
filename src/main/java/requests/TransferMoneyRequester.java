package requests;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.requests.TransferMoneyRequest;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class TransferMoneyRequester extends Request<TransferMoneyRequest> {
    public TransferMoneyRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(TransferMoneyRequest model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .post("/api/v1/accounts/transfer")
                .then()
                .spec(responseSpecification);
    }

    @Override
    public ValidatableResponse get() {
        return null;
        //TODO
    }
}
