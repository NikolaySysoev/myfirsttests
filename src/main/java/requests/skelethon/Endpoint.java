package requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.BaseModel;
import models.requests.*;
import models.responses.*;

@Getter
@AllArgsConstructor
public enum Endpoint {
    ADMIN_CREATE_USERS(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),
    LOGIN(
            "/auth/login",
            LoginRequest.class,
            LoginResponse.class
    ),
    CREATE_ACCOUNTS(
            "/accounts",
            CreateAccountRequest.class, //в лекции BaseModel.class
            CreateAccountResponse.class
    ),
    DEPOSIT_MONEY (
            "/accounts/deposit",
            DepositMoneyRequest.class,
            DepositMoneyResponse.class
    ),
    GET_CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            GetUserAccountsRequest.class,
            GetUserAccountsResponse.class
    ),
    ACCOUNTS_TRANSFER(
            "/accounts/transfer",
            TransferMoneyRequest.class,
            TransferMoneyResponse.class
    ),
    GET_CUSTOMER_PROFILE(
            "/customer/profile",
            GetCustomerProfileRequest.class,
            GetCustomerProfileResponse.class
    ),
    CHANGE_CUSTOMER_NAME(
            "/customer/profile",
            ChangeNameRequest.class,
            ChangeNameResponse.class
    ),
    ACCOUNTS_DEPOSIT(
            "/accounts/deposit",
            DepositMoneyRequest.class,
            DepositMoneyResponse.class
    );

    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends  BaseModel> responseModel;
}
