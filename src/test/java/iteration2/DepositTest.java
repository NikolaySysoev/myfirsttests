package iteration2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(
                        new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> depositValidData() {
        return Stream.of(
                Arguments.of(4999.99),
                Arguments.of(0.01),
                Arguments.of(5000.00));
    }

    public static Stream<Arguments> depositInvalidData() {
        return Stream.of(
                Arguments.of(0.00, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(5000.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(-0.01, HttpStatus.SC_BAD_REQUEST),
                Arguments.of("100", HttpStatus.SC_INTERNAL_SERVER_ERROR),
                Arguments.of(null, HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    public static Stream<Arguments> depositInvalidAccount() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999999, HttpStatus.SC_BAD_REQUEST));
    }

    @ParameterizedTest
    @MethodSource("depositValidData")
    public void userCanDepositOnHisAccount(double balance) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String userName = UUID.randomUUID().toString().substring(0,8);

        String createUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, userName);

        //создаем пользователя и тащим токен
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица пользователя и вытаскиваем его айди
        Integer userAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float initialBalance = 0.00f;

        //создаем тело для пост запроса на пополнение баланса
        String requestBody = String.format("""
                {
                    "id": %s,
                        "balance": %s
                }
                """,userAccountId, balance);

        //делаем запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        //делаем гет запрос и вытаскиваем текущий баланс
        Float balanceAfterDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .body()
                .path("[0].balance");

        //сравниваем стартовый баланс (0) и текущий с погрешностью в 1 копейку
        assertEquals(initialBalance + balance, balanceAfterDeposit, 0.01);
    }

    @ParameterizedTest
    @MethodSource("depositInvalidData")
    public void userCanNotDepositOnHisAccountWithInvalidData(Object balance, int statusCode) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String userName = UUID.randomUUID().toString().substring(0,8);

        String createUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, userName);

        //создаем пользователя и тащим токен
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        //создаем счет от лица пользователя и вытаскиваем его айди
        Integer userAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .path("id");

        float initialBalance = 0.00f;

        //блок IF для преведения числа 100 из 3го кейса к строке.
        //без него строковое значение собиралось как число
        String balanceValue;

        if (balance instanceof String) {
            balanceValue = "\"" + balance + "\"";
        } else {
            balanceValue = String.valueOf(balance);
        }

        String requestBody = String.format("""
                {
                    "id": %s,
                        "balance": %s
                }
                """,userAccountId, balanceValue);

        //пост запрос на депозит
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(statusCode);

        //проверка баланса
        Float balanceAfterDeposit = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .extract()
                .body()
                .path("[0].balance");

        assertEquals(initialBalance, balanceAfterDeposit, 0.01);
    }

    @ParameterizedTest
    @MethodSource("depositInvalidAccount")
    public void userCanNotDepositOnInvalidAccount(int accountId) {
        //создаем пользователя от лица админа
        //генерация рандомного имени длиной 8 символов
        String userName = UUID.randomUUID().toString().substring(0,8);

        String createUserBody = String.format("""
                {
                            "username": "%s",
                                "password": "verysTRongPassword33$",
                                "role": "USER"
                        }
                """, userName);

        //создаем пользователя и тащим токен
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(createUserBody)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .header("Authorization");

        String requestBody = String.format("""
                {
                    "id": %s,
                    "balance": 100
                }
                """, accountId);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthToken)
                .body(requestBody)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
