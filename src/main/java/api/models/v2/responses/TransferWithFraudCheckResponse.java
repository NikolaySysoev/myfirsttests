package api.models.v2.responses;

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
public class TransferWithFraudCheckResponse extends BaseModel {
    private String status;
    private String message;
    private BigDecimal amount;
    private long senderAccountId;
    private long receiverAccountId;
    private double fraudRiskScore;
    private String fraudReason;
    private boolean requiresManualReview;
    private boolean requiresVerification;
    private long transactionId;
}
