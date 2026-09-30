package api.models.v2.responses;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeNameResponse extends BaseModel {
    private String message;
    private CustomerResponse customer;
}
