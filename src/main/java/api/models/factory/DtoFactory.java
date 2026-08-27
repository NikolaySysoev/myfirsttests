package api.models.factory;

import api.configs.BackendVersion;
import api.models.BaseModel;
import api.models.domain.Account;
import api.models.domain.ApiError;
import api.models.domain.CustomerProfile;
import api.models.domain.ExpectedError;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Абстрактная фабрика DTO: собирает запросы под активную версию контракта
 * и разбирает ответы этой версии в нейтральные доменные объекты.
 * <p>
 * Тест описывает всё в доменных терминах ("депозит на счёт X суммы Y",
 * "счёт с таким-то балансом") и не знает о существовании пакетов
 * {@code api.models.v1} / {@code api.models.v2}. Весь версионный код живёт
 * в двух реализациях этого интерфейса.
 * <p>
 * Запросы возвращаются как {@link BaseModel}: DTO запроса из теста только
 * уходит наружу, {@code CrudRequester.post(BaseModel)} принимает именно его.
 * Ответы, наоборот, нужно читать — поэтому они разбираются в
 * {@link Account} / {@link CustomerProfile}.
 */
public interface DtoFactory {

    /**
     * Единственный switch по версии во всём проекте.
     * При добавлении новой версии в {@link BackendVersion} компилятор укажет сюда.
     */
    static DtoFactory of(BackendVersion version) {
        return switch (version) {
            case V1 -> new V1DtoFactory();
            case V2 -> new V2DtoFactory();
        };
    }

    // ---------- запросы ----------

    /**
     * Запрос на пополнение счёта.
     *
     * @param accountId идентификатор счёта, который пополняем
     * @param amount    сумма пополнения
     */
    BaseModel deposit(long accountId, BigDecimal amount);

    /**
     * Запрос на смену имени профиля.
     * <p>
     * Тело запроса у версий пока одинаковое, но метод всё равно живёт здесь:
     * тест не должен импортировать версионные пакеты, а если контракт разойдётся,
     * правка останется внутри фабрики.
     */
    BaseModel changeName(String name);

    // ---------- ответы ----------

    /** Ответ на создание счёта -> нейтральный счёт. */
    Account createdAccount(BaseModel createAccountResponse);

    /** Ответ со списком счетов пользователя -> нейтральные счета. */
    List<Account> accounts(BaseModel[] getUserAccountsResponses);

    /** Ответ с профилем пользователя -> нейтральный профиль. */
    CustomerProfile customerProfile(BaseModel getCustomerProfileResponse);

    /** Ответ на смену имени -> нейтральный профиль. */
    CustomerProfile changedName(BaseModel changeNameResponse);

    /**
     * Сообщение об успехе из ответа на смену имени.
     * <p>
     * Легаси отдаёт "Profile updated successfully", в актуальной версии такого поля
     * в ответе нет вовсе — поэтому {@link Optional}, а не строка.
     */
    Optional<String> successMessage(BaseModel changeNameResponse);

    // ---------- ошибки ----------

    /**
     * Как логическая ошибка выглядит в этой версии: текст и место в теле ответа.
     * <p>
     * Версии расходятся сразу по двум осям — формулировка ("cannot exceed 5000"
     * против "exceeds the 5000 limit") и оболочка (голая строка, {@code message}
     * или список сообщений по имени невалидного поля). Обе оси описаны здесь,
     * поэтому тесты оперируют только ключом {@link ApiError}.
     */
    ExpectedError expect(ApiError error);
}
