package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.interfaces.CrudEndpointInterface;

//используется для позитивных кейсов
public class ValidatedCrudRequester<M extends BaseModel> extends HttpRequest implements CrudEndpointInterface {
    private CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public M post(BaseModel model) {
        return (M) crudRequester.post(model)
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public Object get(long id) {
        return null;
    }

    @Override
    public M get() {
        return (M) crudRequester.get()
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public Object put(long id, BaseModel model) {
        return null;
    }

    @Override
    public M put(BaseModel model) {
        return (M) crudRequester.put(model)
                .extract()
                .as(endpoint.getResponseModel());
    }

    @Override
    public Object delete(long id) {
        return null;
    }
}
