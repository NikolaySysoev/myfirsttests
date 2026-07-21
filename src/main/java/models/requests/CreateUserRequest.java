package models.requests;

import generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.BaseModel;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserRequest extends BaseModel {
    @GeneratingRule("[a-zA-Z0-9._-]{6,15}")
    private String username;
    @GeneratingRule("[a-z][A-Z][0-9][!@#$%^&+=-][A-Za-z0-9!@#$%^&*_+=-]{4,16}")
    private String password;
    @GeneratingRule("USER")
    private String role;
}
