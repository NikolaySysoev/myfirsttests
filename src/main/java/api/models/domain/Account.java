package api.models.domain;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Счёт в терминах теста, без привязки к версии контракта.
 * <p>
 * Собирается фабрикой {@code DtoFactory} из версионной модели ответа
 * ({@code CreateAccountResponse}, {@code GetUserAccountsResponse}, ...).
 * Тесты и шаги работают только с этим типом, поэтому расхождение контрактов
 * не протекает в тестовый код.
 * <p>
 * Имена геттеров намеренно совпадают с прежними моделями ответов
 * ({@code getId}, {@code getAccountNumber}, {@code getBalance}) — благодаря
 * этому переход на нейтральный тип не потребовал правок в тестах.
 */
@Data
public class Account {
    private final long id;
    private final String accountNumber;
    private final BigDecimal balance;
}
