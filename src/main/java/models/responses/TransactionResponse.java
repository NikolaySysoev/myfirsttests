package models.responses;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private long id;
    private BigDecimal amount;
    private String type;

    @JsonFormat(pattern = "EEE MMM dd HH:mm:ss zzz yyyy", locale = "en")
    private Date timestamp;

    private long relatedAccountId;
}