package models.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import models.BaseModel;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransferMoneyRequest extends BaseModel {
    long senderAccountId;
    long receiverAccountId;
    BigDecimal amount;
}