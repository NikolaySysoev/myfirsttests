package models;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import requests.Request;

@Data
@AllArgsConstructor
@Builder
public class CreateAccountUserRequest extends BaseModel {

}
