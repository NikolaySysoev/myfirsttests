package api.models.v2.requests;

import api.models.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferWithFraudCheckRequest extends BaseModel {
    private long senderAccountId;
    private long receiverAccountId;
    private BigDecimal amount;
}
