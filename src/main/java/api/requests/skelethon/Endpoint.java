package api.requests.skelethon;

import api.configs.BackendVersion;
import api.models.BaseModel;
import api.models.v1.requests.*;
import api.models.v1.responses.*;
import common.versioning.ApiVersionContext;

import java.util.Map;

import static api.requests.skelethon.Contract.sameForAllVersions;

/**
 * Реестр эндпоинтов и их контрактов по версиям бэкенда.
 * <p>
 * Каждая константа хранит мапу "версия -> {@link Contract}". Геттеры
 * ({@link #getUrl()}, {@link #getRequestModel()}, {@link #getResponseModel()})
 * отдают контракт активной версии, поэтому реквестеры и спеки, которые их
 * вызывают, о версиях ничего не знают и менять их не пришлось.
 * <p>
 * Эндпоинты, у которых новая версия пока не разошлась со старой, описаны через
 * {@link Contract#sameForAllVersions}. По мере появления v2-моделей такие строки
 * заменяются явной мапой — см. {@link #ACCOUNTS_DEPOSIT}.
 */
public enum Endpoint {

    ADMIN_CREATE_USERS(sameForAllVersions(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    )),
    LOGIN(sameForAllVersions(
            "/auth/login",
            LoginRequest.class,
            LoginResponse.class
    )),
    /** Запрос без тела, разошлись только ответы (формат даты в транзакциях). */
    CREATE_ACCOUNTS(Map.of(
            BackendVersion.V1, new Contract(
                    "/accounts",
                    CreateAccountRequest.class,
                    api.models.v1.responses.CreateAccountResponse.class),
            BackendVersion.V2, new Contract(
                    "/accounts",
                    CreateAccountRequest.class,
                    api.models.v2.responses.CreateAccountResponse.class)
    )),
    GET_CUSTOMER_ACCOUNTS(Map.of(
            BackendVersion.V1, new Contract(
                    "/customer/accounts",
                    GetUserAccountsRequest.class,
                    api.models.v1.responses.GetUserAccountsResponse.class),
            BackendVersion.V2, new Contract(
                    "/customer/accounts",
                    GetUserAccountsRequest.class,
                    api.models.v2.responses.GetUserAccountsResponse.class)
    )),
    ACCOUNTS_TRANSFER(sameForAllVersions(
            "/accounts/transfer",
            TransferMoneyRequest.class,
            TransferMoneyResponse.class
    )),
    GET_CUSTOMER_PROFILE(Map.of(
            BackendVersion.V1, new Contract(
                    "/customer/profile",
                    GetCustomerProfileRequest.class,
                    api.models.v1.responses.GetCustomerProfileResponse.class),
            BackendVersion.V2, new Contract(
                    "/customer/profile",
                    GetCustomerProfileRequest.class,
                    api.models.v2.responses.GetCustomerProfileResponse.class)
    )),
    /** Тело запроса не изменилось, ответ разошёлся: обёртка с message -> плоский клиент. */
    CHANGE_CUSTOMER_NAME(Map.of(
            BackendVersion.V1, new Contract(
                    "/customer/profile",
                    ChangeNameRequest.class,
                    api.models.v1.responses.ChangeNameResponse.class),
            BackendVersion.V2, new Contract(
                    "/customer/profile",
                    ChangeNameRequest.class,
                    api.models.v2.responses.ChangeNameResponse.class)
    )),

    /** Контракты разошлись: у V1 id/balance, у V2 accountId/amount. */
    ACCOUNTS_DEPOSIT(Map.of(
            BackendVersion.V1, new Contract(
                    "/accounts/deposit",
                    api.models.v1.requests.DepositMoneyRequest.class,
                    api.models.v1.responses.DepositMoneyResponse.class),
            BackendVersion.V2, new Contract(
                    "/accounts/deposit",
                    api.models.v2.requests.DepositMoneyRequest.class,
                    api.models.v2.responses.DepositMoneyResponse.class)
    ));

    private final Map<BackendVersion, Contract> contracts;

    Endpoint(Map<BackendVersion, Contract> contracts) {
        this.contracts = contracts;
    }

    public String getUrl() {
        return contract().getUrl();
    }

    public Class<? extends BaseModel> getRequestModel() {
        return contract().getRequestModel();
    }

    public Class<? extends BaseModel> getResponseModel() {
        return contract().getResponseModel();
    }

    /**
     * Класс массива моделей ответа активной версии — для эндпоинтов, которые
     * отдают JSON-массив и разбираются через {@code .as(...)} напрямую,
     * минуя {@code ValidatedCrudRequester}.
     */
    public Class<?> getResponseArrayModel() {
        return java.lang.reflect.Array.newInstance(getResponseModel(), 0).getClass();
    }

    /** Контракт, соответствующий версии текущего теста. */
    private Contract contract() {
        BackendVersion version = ApiVersionContext.current();
        Contract contract = contracts.get(version);
        if (contract == null) {
            throw new IllegalStateException(
                    "Для эндпоинта " + name() + " не описан контракт версии " + version
                            + ". Добавьте его в Endpoint.");
        }
        return contract;
    }
}
