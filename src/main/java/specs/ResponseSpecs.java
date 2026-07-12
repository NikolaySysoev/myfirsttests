package specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;

public class ResponseSpecs {
    private ResponseSpecs(){}

    private static ResponseSpecBuilder defaultResponseBuilder(){
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification entityWasCreated(){
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK(){
        return defaultResponseBuilder().
                expectStatusCode(HttpStatus.SC_OK)
                .build();
    }
}
