package api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import api.models.BaseModel;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse extends BaseModel {
    private String username;
    private String role;
}
