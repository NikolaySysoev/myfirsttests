package api.requests.steps;

import api.models.BaseModel;
import api.models.domain.Account;
import api.models.domain.CustomerProfile;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.versioning.ApiVersionContext;

import java.math.BigDecimal;
import java.util.List;

/**
 * Действия от лица пользователя.
 * <p>
 * Шаги не зависят от версии бэкенда: модели запроса и ответа выбирает
 * {@link Endpoint} по активной версии, а разбор ответа в нейтральные
 * {@link Account} / {@link CustomerProfile} делает фабрика.
 */
public class UserSteps {
    private final String username;
    private final String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Account createAccount() {
        BaseModel response = new ValidatedCrudRequester<BaseModel>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CREATE_ACCOUNTS,
                ResponseSpecs.entityWasCreated()
        )
                .post();

        return ApiVersionContext.dto().createdAccount(response);
    }

    /**
     * Пополнение счёта. DTO запроса собирает фабрика активной версии, модель ответа
     * приходит из контракта {@link Endpoint}.
     * <p>
     * Возвращается {@link BaseModel}: поля ответа сейчас никто не читает — баланс
     * тесты берут через {@link #getAccountBalance(long)}. Понадобится читать —
     * добавим разбор в фабрику, как для счетов и профиля.
     */
    public BaseModel depositMoney(long accountId, BigDecimal amount) {
        var request = ApiVersionContext.dto().deposit(accountId, amount);

        return new ValidatedCrudRequester<BaseModel>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK()
        )
                .post(request);
    }

    public List<Account> getAccounts() {
        BaseModel[] response = (BaseModel[]) new CrudRequester(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK()
        )
                .get()
                .extract()
                .as(Endpoint.GET_CUSTOMER_ACCOUNTS.getResponseArrayModel());

        return ApiVersionContext.dto().accounts(response);
    }

    public BigDecimal getAccountBalance(long accountId) {
        return getAccountBalance(getAccounts(), accountId);
    }

    public CustomerProfile getCustomerProfile() {
        BaseModel response = new ValidatedCrudRequester<BaseModel>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK()
        )
                .get();

        return ApiVersionContext.dto().customerProfile(response);
    }

    // приватный хелпер: чистая фильтрация уже полученного списка счетов, без похода в API
    private static BigDecimal getAccountBalance(List<Account> accounts, long accountId) {
        return accounts.stream()
                .filter(account -> account.getId() == accountId)
                .map(Account::getBalance)
                .findFirst()
                .orElseThrow();
    }
}
