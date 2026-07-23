package requests.steps;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.ResponseSpecification;
import models.requests.CreateUserRequest;
import models.requests.DepositMoneyRequest;
import models.requests.LoginRequest;
import models.responses.CreateAccountResponse;
import models.responses.DepositMoneyResponse;
import models.responses.GetCustomerProfileResponse;
import models.responses.GetUserAccountsResponse;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

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
                .header("authorization");
    }

    public static CreateAccountResponse createAccount(String username, String password) {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post(null);
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

