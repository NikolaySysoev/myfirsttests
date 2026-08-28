package api.models.domain;

import lombok.Data;

import java.util.List;

/**
 * Профиль пользователя в терминах теста, без привязки к версии контракта.
 * <p>
 * Поле с паролем намеренно не переносится: в ответе приходит хэш, и ни один
 * тест его не читает.
 */
@Data
public class CustomerProfile {
    private final long id;
    private final String username;
    private final String name;
    private final String role;
    private final List<Account> accounts;
}
