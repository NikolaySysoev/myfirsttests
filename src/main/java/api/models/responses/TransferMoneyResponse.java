package api.models.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import api.models.BaseModel;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferMoneyResponse extends BaseModel {
    String message;
    BigDecimal amount;
    long receiverAccountId;
    long senderAccountId;
}
