package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAccountUserResponse extends BaseModel {
    private long id;
    private String accountNumber;
    private String balance;
    private List<String> transactions;
}
