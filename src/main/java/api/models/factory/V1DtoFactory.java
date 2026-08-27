package api.models.factory;

import api.models.BaseModel;
import api.models.domain.Account;
import api.models.domain.ApiError;
import api.models.domain.CustomerProfile;
import api.models.domain.ExpectedError;
import api.models.v1.requests.ChangeNameRequest;
import api.models.v1.requests.DepositMoneyRequest;
import api.models.v1.responses.AccountsResponse;
import api.models.v1.responses.ChangeNameResponse;
import api.models.v1.responses.CreateAccountResponse;
import api.models.v1.responses.GetCustomerProfileResponse;
import api.models.v1.responses.GetUserAccountsResponse;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Контракты легаси-версии (V1).
 */
public class V1DtoFactory implements DtoFactory {

    @Override
    public BaseModel deposit(long accountId, BigDecimal amount) {
        return DepositMoneyRequest.builder()
                .id(accountId)
                .balance(amount)
                .build();
    }

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

    /** Легаси отдаёт текст ошибки голой строкой в теле ответа. */
    @Override
    public ExpectedError expect(ApiError error) {
        return ExpectedError.plainBody(api.models.v1.ApiError.valueOf(error.name()).getMessage());
    }

    @Override
    public CustomerProfile changedName(BaseModel changeNameResponse) {
        ChangeNameResponse response = (ChangeNameResponse) changeNameResponse;
        var customer = response.getCustomer();
        return new CustomerProfile(
                customer.getId(), customer.getUsername(), customer.getName(), customer.getRole(), List.of());
    }

    @Override
    public Optional<String> successMessage(BaseModel changeNameResponse) {
        return Optional.ofNullable(((ChangeNameResponse) changeNameResponse).getMessage());
    }
}
