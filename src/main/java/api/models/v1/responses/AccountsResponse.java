package api.models.v1.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import api.models.BaseModel;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountsResponse extends BaseModel {
    private long id;
    private String accountNumber;
    private BigDecimal balance;
    private List<TransactionResponse> transactions;
}

