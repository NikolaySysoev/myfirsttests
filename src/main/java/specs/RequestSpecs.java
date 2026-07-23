package specs;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import models.requests.LoginRequest;
import requests.LoginRequester;

import java.util.List;

public class RequestSpecs {
    private RequestSpecs() {
    }

    private static RequestSpecBuilder defaultRequestBuilder() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri("http://localhost:4111");
    }

    public static RequestSpecification unAuthSpec(){
        return defaultRequestBuilder()
                .build();
    }

    public static RequestSpecification adminSpec() {
        return defaultRequestBuilder()
                .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
                .build();
    }

    public static RequestSpecification authAsUser(String username, String password) {
        String userAuthHeader = new LoginRequester(
                RequestSpecs.unAuthSpec(),
                ResponseSpecs.requestReturnsOK()
        )
                .post(LoginRequest.builder().username(username).password(password).build())
                .extract()
                .header("authorization");

        return defaultRequestBuilder()
                .addHeader("Authorization", userAuthHeader)
                .build();
    }

    public static RequestSpecification authAsUser(String userAuthHeader) {
        return defaultRequestBuilder()
                .addHeader("Authorization", userAuthHeader)
                .build();
    }
}
