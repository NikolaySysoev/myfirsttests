package api.requests.steps;

import api.models.requests.DepositMoneyRequest;
import api.models.responses.CreateAccountResponse;
import api.models.responses.DepositMoneyResponse;
import api.models.responses.GetCustomerProfileResponse;
import api.models.responses.GetUserAccountsResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;

public class UserSteps {
    private final String username;
    private final String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public CreateAccountResponse createAccount() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post();
    }

    public DepositMoneyResponse depositMoney(long accountId, BigDecimal balance) {
        var request = DepositMoneyRequest.builder().id(accountId).balance(balance).build();

        return new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        )
                .post(request);
    }

    public GetUserAccountsResponse[] getAccounts() {
        return new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserAccountsResponse[].class);
    }

    public BigDecimal getAccountBalance(long accountId) {
        return getAccountBalance(getAccounts(), accountId);
    }

    public GetCustomerProfileResponse getCustomerProfile() {
        return new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get();
    }

    // приватный хелпер: чистая фильтрация уже полученного массива счетов, без похода в API
    private static BigDecimal getAccountBalance(GetUserAccountsResponse[] accounts, long accountId) {
        return Arrays.stream(accounts)
                .filter(acc -> acc.getId() == accountId)
                .map(GetUserAccountsResponse::getBalance)
                .findFirst()
                .orElseThrow();
    }
}
