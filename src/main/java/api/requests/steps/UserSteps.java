package api.requests.steps;

import api.models.requests.CreateUserRequest;
import api.models.requests.DepositMoneyRequest;
import api.models.requests.LoginRequest;
import api.models.responses.CreateAccountResponse;
import api.models.responses.DepositMoneyResponse;
import api.models.responses.GetCustomerProfileResponse;
import api.models.responses.GetUserAccountsResponse;
import org.apache.http.HttpHeaders;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.math.BigDecimal;
import java.util.Arrays;

public class UserSteps {

    public static String loginUser(CreateUserRequest userRequest) {
        var loginRequest = LoginRequest.builder()
                .username(userRequest.getUsername())
                .password(userRequest.getPassword())
                .build();

        String userAuthToken;

        return userAuthToken = new CrudRequester(
                RequestSpecs.unAuthSpec(),
                Endpoint.LOGIN,
                ResponseSpecs.requestReturnsOK()
        )
                .post(loginRequest)
                .extract()
                .header(HttpHeaders.AUTHORIZATION);
    }

    public static CreateAccountResponse createAccount(String username, String password) {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post();
    }

    public static DepositMoneyResponse depositMoney(long accountId, BigDecimal balance, String username, String password) {
        var request = DepositMoneyRequest.builder().id(accountId).balance(balance).build();

        return new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        ).
                post(request);
    }

    public static GetUserAccountsResponse[] getAccounts(String username, String password) {
        GetUserAccountsResponse[] accounts;

        return accounts = new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(GetUserAccountsResponse[].class);
    }

    public static BigDecimal getAccountBalance(GetUserAccountsResponse[] accounts, long accountId) {
        return Arrays.stream(accounts)
                .filter(acc -> acc.getId() == accountId)
                .map(GetUserAccountsResponse::getBalance)
                .findFirst()
                .orElseThrow();
    }

    public static GetCustomerProfileResponse getCustomerProfile(String username, String password) {
        return new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get();
    }
}

