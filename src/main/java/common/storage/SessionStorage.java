package common.storage;

import api.models.requests.CreateUserRequest;
import api.requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Хранилище пользователей текущего теста.
 * <p>
 * Живёт в рамках одного теста: заполняется в {@code UiUserSessionExtension} /
 * {@code ApiUserSessionExtension} по аннотации {@code @UserSession} и очищается
 * перед созданием пользователей для следующего теста (а для UI-тестов ещё и явно
 * в {@code BaseUiTest.tearDown()}).
 * <p>
 * Для каждого созданного пользователя хранится пара:
 * ключ — {@link CreateUserRequest} (сырые данные: username/password/role),
 * значение — {@link UserSteps} (актор: действия от лица этого пользователя без
 * повторной передачи логина/пароля).
 * <p>
 * Порядковый номер пользователя (1, 2, ...) — это порядок, в котором пользователи
 * были переданы в {@link #addUsers(List)}, то есть порядок их создания через
 * {@code AdminSteps.createUser()}.
 */
public class SessionStorage {
    private static final SessionStorage INSTANCE = new SessionStorage();

    private final LinkedHashMap<CreateUserRequest, UserSteps> userStepsMap = new LinkedHashMap<>();

    private SessionStorage() {};

    /**
     * Регистрирует созданных пользователей в хранилище: для каждого создаёт
     * своего актора {@link UserSteps} (на основе его username/password) и
     * кладёт пару "данные -> актор" в хранилище, сохраняя порядок вставки.
     *
     * @param users пользователи в том порядке, в котором их нужно пронумеровать
     *              (первый в списке становится пользователем №1)
     */
    public static void addUsers(List<CreateUserRequest> users) {
        for (CreateUserRequest user: users) {
            INSTANCE.userStepsMap.put(user, new UserSteps(user.getUsername(), user.getPassword()));
        }
    }

    /**
     * Сырые данные пользователя (username/password/role) по его порядковому номеру —
     * когда нужны сами данные, а не действия от его лица. Например, чтобы собрать
     * заголовок авторизации через {@code RequestSpecs.authAsUser(CreateUserRequest)}.
     * <p>
     * Если нужно выполнить действие от лица пользователя (создать счёт, сделать
     * депозит и т.д.) — используйте {@link #actAsUser(int)}, а не этот метод.
     *
     * @param userNumber порядковый номер, начиная с 1 (а не с нуля)
     * @return данные пользователя, соответствующего указанному порядковому номеру
     */
    public static CreateUserRequest getUserRawData (int userNumber) {
        return new ArrayList<>(INSTANCE.userStepsMap.keySet()).get(userNumber -1);
    }

    /**
     * Сырые данные первого (и в большинстве тестов единственного) пользователя.
     * Эквивалентно {@code getUserRawData(1)}.
     */
    public static CreateUserRequest getUserRawData() {
        return getUserRawData(1);
    }

    /**
     * Актор {@link UserSteps} для пользователя с указанным порядковым номером —
     * через него выполняются действия от его лица ({@code createAccount()},
     * {@code depositMoney(...)}, {@code getAccountBalance(...)} и т.д.
     * без повторной передачи логина/пароля.
     *
     * @param userNumber порядковый номер пользователя, начиная с 1
     * @return актор, привязанный к этому пользователю
     */
    public static UserSteps actAsUser(int userNumber) {
        return new ArrayList<>(INSTANCE.userStepsMap.values()).get(userNumber-1);
    }

    /**
     * Актор первого (и в большинстве тестов единственного) пользователя.
     * Эквивалентно {@code actAsUser(1)}.
     */
    public static UserSteps actAsUser() {
        return actAsUser(1);
    }

    /**
     * Полностью очищает хранилище пользователей.
     */
    public static void clear() {
        INSTANCE.userStepsMap.clear();
    }
}
