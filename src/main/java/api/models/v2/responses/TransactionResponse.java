package api.models.v2.responses;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import common.json.IsoDateTimeDeserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import api.models.BaseModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse extends BaseModel {
    private long id;
    private BigDecimal amount;
    private String type;
    @JsonDeserialize(using = IsoDateTimeDeserializer.class)
    private LocalDateTime timestamp;
    private long relatedAccountId;
}