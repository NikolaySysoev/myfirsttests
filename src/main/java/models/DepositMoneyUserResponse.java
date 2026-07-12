package models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepositMoneyUserResponse {
    private long id;
    private String accountNumber;
    private BigDecimal balance;
    private List<TransactionResponse> transactions;
}

/*
 * {
 *   "id": 1,
 *   "accountNumber": "ACC1",
 *   "balance": 100.5,
 *   "transactions": [
 *     {
 *       "id": 1,
 *       "amount": 100.5,
 *       "type": "DEPOSIT",
 *       "timestamp": "Sun Jul 12 13:05:47 UTC 2026",
 *       "relatedAccountId": 1
 *     }
 *   ]
 * }
 */