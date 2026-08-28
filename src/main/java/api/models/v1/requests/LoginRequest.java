package api.models.v1.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import api.models.BaseModel;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest extends BaseModel {
    private String username;
    private String password;
}
