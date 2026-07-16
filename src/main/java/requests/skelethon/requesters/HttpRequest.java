package requests.skelethon.requesters;

import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.Endpoint;

public abstract class HttpRequest {
    protected RequestSpecification requestSpecification;
    protected Endpoint endpoint;
    protected ResponseSpecification responseSpecification;

    public HttpRequest(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        this.requestSpecification = requestSpecification;
        this.endpoint = endpoint;
        this.responseSpecification = responseSpecification;
    }
}
