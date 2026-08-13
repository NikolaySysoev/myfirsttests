package api.specs;

import api.configs.Config;
import api.models.requests.CreateUserRequest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import api.models.requests.LoginRequest;
import org.apache.http.HttpHeaders;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestSpecs {
    private static Map<String, String> authHeaders = new HashMap<>(Map.of(Config.getProperty("adminLogin"), Config.getProperty("adminToken")));

    private RequestSpecs() {
    }

    private static RequestSpecBuilder defaultRequestBuilder() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilters(List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()))
                .setBaseUri(Config.getProperty("apiBaseUrl") + Config.getProperty("apiVersion"));
    }

    public static RequestSpecification unAuthSpec() {
        return defaultRequestBuilder()
                .build();
    }

    public static RequestSpecification adminSpec() {
        return defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, Config.getProperty("adminToken"))
                .build();
    }

    public static RequestSpecification authAsUser(String username, String password) {
        return defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, getUserAuthHeader(username, password))
                .build();
    }

    public static RequestSpecification authAsUser(CreateUserRequest user) {
        return authAsUser(user.getUsername(), user.getPassword());
    }

    public static RequestSpecification authAsUser(String userAuthHeader) {
        return defaultRequestBuilder()
                .addHeader(HttpHeaders.AUTHORIZATION, userAuthHeader)
                .build();
    }

    public static String getUserAuthHeader(String username, String password) {
        String userAuthHeader;

        if (!authHeaders.containsKey(username)) {
            userAuthHeader = new CrudRequester(
                    RequestSpecs.unAuthSpec(),
                    Endpoint.LOGIN,
                    ResponseSpecs.requestReturnsOK()
            )
                    .post(LoginRequest.builder().username(username).password(password).build())
                    .extract()
                    .header(HttpHeaders.AUTHORIZATION);

            authHeaders.put(username, userAuthHeader);
        } else {
            userAuthHeader = authHeaders.get(username);
        }

        return userAuthHeader;
    }
}
