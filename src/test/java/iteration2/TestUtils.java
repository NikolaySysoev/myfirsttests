package iteration2;

import api.models.responses.GetUserAccountsResponse;

import java.math.BigDecimal;
import java.util.Arrays;

public class TestUtils {
    public static void repeat(int times, Runnable action){
        for (int i = 0; i < times; i++){
            action.run();
        }
    }

    //хэлпер для получения баланса пользователя
    public static BigDecimal getAccountBalance(GetUserAccountsResponse[] accounts, long accountId) {
        return Arrays.stream(accounts)
                .filter(acc -> acc.getId() == accountId)
                .map(GetUserAccountsResponse::getBalance)
                .findFirst()
                .orElseThrow();
    }
}
