package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.interfaces.CrudEndpointInterface;

import static io.restassured.RestAssured.given;

//используется для негативных кейсов
public class CrudRequester extends HttpRequest implements CrudEndpointInterface {
    public CrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public ValidatableResponse post(BaseModel model) {
        var body = model == null ? "" : model;

        return given()
                .spec(requestSpecification)
                .body(body)
                .post(endpoint.getUrl())
                .then()
                .assertThat()
                .spec(responseSpecification);
    }

    @Override
    public Object get(long id) {
        return null;
    }

    @Override
    public ValidatableResponse get() {
        return given()
                .spec(requestSpecification)
                .get(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public Object put(long id, BaseModel model) {
        return null;
    }

    @Override
    public ValidatableResponse put(BaseModel model) {
        return given()
                .spec(requestSpecification)
                .body(model)
                .put(endpoint.getUrl())
                .then()
                .spec(responseSpecification);
    }

    @Override
    public Object delete(long id) {
        return null;
    }
}
