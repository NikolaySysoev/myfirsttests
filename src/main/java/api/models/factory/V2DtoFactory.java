package api.models.factory;

import api.models.BaseModel;
import api.models.domain.Account;
import api.models.domain.ApiError;
import api.models.domain.CustomerProfile;
import api.models.domain.ExpectedError;
import api.models.v1.requests.ChangeNameRequest;
import api.models.v2.requests.DepositMoneyRequest;
import api.models.v2.responses.AccountsResponse;
import api.models.v2.responses.ChangeNameResponse;
import api.models.v2.responses.CreateAccountResponse;
import api.models.v2.responses.GetCustomerProfileResponse;
import api.models.v2.responses.GetUserAccountsResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Контракты актуальной версии (V2).
 */
public class V2DtoFactory implements DtoFactory {

    @Override
    public BaseModel deposit(long accountId, BigDecimal amount) {
        return DepositMoneyRequest.builder()
                .accountId(accountId)
                .amount(amount)
                .build();
    }

    /** Тело запроса не разошлось с легаси, поэтому переиспользуем ту же модель. */
    @Override
    public BaseModel changeName(String name) {
        return ChangeNameRequest.builder().name(name).build();
    }

    @Override
    public Account createdAccount(BaseModel createAccountResponse) {
        CreateAccountResponse response = (CreateAccountResponse) createAccountResponse;
        return new Account(response.getId(), response.getAccountNumber(), response.getBalance());
    }

    @Override
    public List<Account> accounts(BaseModel[] getUserAccountsResponses) {
        return Arrays.stream(getUserAccountsResponses)
                .map(GetUserAccountsResponse.class::cast)
                .map(response -> new Account(
                        response.getId(), response.getAccountNumber(), response.getBalance()))
                .toList();
    }

    @Override
    public CustomerProfile customerProfile(BaseModel getCustomerProfileResponse) {
        GetCustomerProfileResponse response = (GetCustomerProfileResponse) getCustomerProfileResponse;
        List<AccountsResponse> accounts =
                response.getAccounts() == null ? List.of() : response.getAccounts();

        return new CustomerProfile(
                response.getId(),
                response.getUsername(),
                response.getName(),
                response.getRole(),
                accounts.stream()
                        .map(account -> new Account(
                                account.getId(), account.getAccountNumber(), account.getBalance()))
                        .toList());
    }

    /** Тексты и пути описаны в {@link api.models.v2.ApiError}. */
    @Override
    public ExpectedError expect(ApiError error) {
        api.models.v2.ApiError versioned = api.models.v2.ApiError.valueOf(error.name());
        return ExpectedError.atPath(versioned.getJsonPath(), versioned.getMessage());
    }

    @Override
    public String successMessage(BaseModel changeNameResponse) {
        return ((ChangeNameResponse) changeNameResponse).getMessage();
    }
}
