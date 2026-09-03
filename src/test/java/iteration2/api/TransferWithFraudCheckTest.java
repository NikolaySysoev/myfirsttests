package iteration2.api;

import api.dao.checks.DbChecks;
import api.models.assertions.ModelAssertions;
import api.models.v2.requests.TransferWithFraudCheckRequest;
import api.models.v2.responses.TransferWithFraudCheckResponse;
import api.requests.steps.DataBaseSteps;
import common.annotations.ApiVersion;
import common.annotations.FraudCheckMock;
import common.annotations.UserSession;
import common.extensions.FraudCheckWireMockExtension;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static api.configs.BackendVersion.V2;
import static common.annotations.FraudCheckMock.Behaviour.CONNECTION_ERROR;
import static common.annotations.FraudCheckMock.Behaviour.HTTP_ERROR;
import static common.annotations.FraudCheckMock.Behaviour.SERVICE_DOWN;

@ApiVersion(V2)
@ExtendWith(FraudCheckWireMockExtension.class)
public class TransferWithFraudCheckTest extends BaseApiTest {

    private final BigDecimal amountToDeposit =
            new BigDecimal(Math.random() * 4999.9 + 0.1).setScale(2, RoundingMode.HALF_UP);
    private BigDecimal transferAmount;

    private long senderAccountId;
    private long receiverAccountId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal senderBalanceAfterSetup;
    private BigDecimal receiverBalanceAfterSetup;

    @BeforeEach
    public void setup() {
        // пользователи уже созданы ApiUserSessionExtension'ом (по @UserSession на тестовом методе)
        // отправитель — счёт первого пользователя, получатель — счёт второго
        var senderAccount = SessionStorage.actAsUser().createAccount();
        var receiverAccount = SessionStorage.actAsUser(2).createAccount();

        senderAccountId = senderAccount.getId();
        receiverAccountId = receiverAccount.getId();
        senderAccountNumber = senderAccount.getAccountNumber();
        receiverAccountNumber = receiverAccount.getAccountNumber();

        // деньги для будущих переводов
        SessionStorage.actAsUser().depositMoney(senderAccountId, amountToDeposit);

        // переводим случайную часть от того, что положили, но не меньше 0.1
        BigDecimal min = new BigDecimal("0.1");
        transferAmount = amountToDeposit
                .subtract(min)
                .multiply(BigDecimal.valueOf(Math.random()))
                .add(min)
                .setScale(2, RoundingMode.HALF_UP);

        senderBalanceAfterSetup = SessionStorage.actAsUser().getAccountBalance(senderAccountId);
        receiverBalanceAfterSetup = SessionStorage.actAsUser(2).getAccountBalance(receiverAccountId);
    }

    @Test
    @UserSession(2)
    @DisplayName("APPROVED: перевод проходит сразу")
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "APPROVED",
            riskScore = 0.2,
            reason = "Low risk transaction",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void transferIsApprovedWhenFraudServiceReturnsApproved(DbChecks db) {
        var transferRequest = new TransferWithFraudCheckRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        var transferResponse = SessionStorage.actAsUser().transferWithFraudCheck(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount()
        );

        // эхо: бэк вернул те же счета и сумму, что мы отправили
        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        var expectedResponse = TransferWithFraudCheckResponse.builder()
                .status("APPROVED")
                .message("Transfer approved and processed immediately")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.2)
                .fraudReason("Low risk transaction")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        // результат фрод-проверки: сверяем ответ целиком с эталоном.
        // transactionId генерирует бэк, в эталоне его нет — исключаем.
        // BigDecimal.equals учитывает scale (2141.13 != 2141.130), поэтому
        // для этого типа сравниваем через compareTo.
        softly.assertThat(transferResponse)
                .usingRecursiveComparison()
                .ignoringFields("transactionId")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedResponse);

        assertMoneyMoved(transferAmount, db);
    }

    @Test
    @UserSession(2)
    @DisplayName("BLOCKED: перевод заблокирован, деньги не двигаются")
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "BLOCKED",
            riskScore = 0.95,
            reason = "High risk transaction",
            requiresManualReview = false,
            additionalVerificationRequired = false
    )
    public void transferIsBlockedWhenFraudServiceReturnsBlocked(DbChecks db) {
        var transferRequest = new TransferWithFraudCheckRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        var transferResponse = SessionStorage.actAsUser().transferWithFraudCheck(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount()
        );

        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        var expectedResponse = TransferWithFraudCheckResponse.builder()
                .status("BLOCKED")
                .message("Transfer blocked due to fraud detection")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.95)
                .fraudReason("High risk transaction")
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        softly.assertThat(transferResponse)
                .usingRecursiveComparison()
                .ignoringFields("transactionId")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedResponse);

        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("REVIEW_REQUIRED: перевод уходит на ручную проверку")
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "REVIEW_REQUIRED",
            riskScore = 0.6,
            reason = "Suspicious activity detected",
            requiresManualReview = true,
            additionalVerificationRequired = false
    )
    public void transferGoesToManualReviewWhenFraudServiceAsksForReview(DbChecks db) {
        var transferRequest = new TransferWithFraudCheckRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        var transferResponse = SessionStorage.actAsUser().transferWithFraudCheck(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount()
        );

        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        var expectedResponse = TransferWithFraudCheckResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.6)
                .fraudReason("Suspicious activity detected")
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        softly.assertThat(transferResponse)
                .usingRecursiveComparison()
                .ignoringFields("transactionId")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedResponse);

        // до ручной проверки деньги не переводятся
        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("VERIFICATION_REQUIRED: требуется дополнительное подтверждение")
    @FraudCheckMock(
            status = "SUCCESS",
            decision = "VERIFICATION_REQUIRED",
            riskScore = 0.45,
            reason = "Additional verification needed",
            requiresManualReview = false,
            additionalVerificationRequired = true
    )
    public void transferRequiresVerificationWhenFraudServiceAsksForIt(DbChecks db) {
        var transferRequest = new TransferWithFraudCheckRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        var transferResponse = SessionStorage.actAsUser().transferWithFraudCheck(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount()
        );

        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        var expectedResponse = TransferWithFraudCheckResponse.builder()
                .status("VERIFICATION_REQUIRED")
                .message("Additional verification required")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.45)
                .fraudReason("Additional verification needed")
                .requiresManualReview(false)
                .requiresVerification(true)
                .build();

        softly.assertThat(transferResponse)
                .usingRecursiveComparison()
                .ignoringFields("transactionId")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedResponse);

        // до подтверждения деньги не переводятся
        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис отвечает 500: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = HTTP_ERROR, httpStatus = 500)
    public void transferFallsBackToManualReviewWhenFraudServiceReturnsError(DbChecks db) {
        assertFallsBackToManualReview("Unexpected error during fraud check: 500", db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис рвёт соединение: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = CONNECTION_ERROR)
    public void transferFallsBackToManualReviewWhenConnectionIsReset(DbChecks db) {
        assertFallsBackToManualReview("Fraud detection service is currently unavailable", db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис не поднят: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = SERVICE_DOWN)
    public void transferFallsBackToManualReviewWhenServiceIsDown(DbChecks db) {
        assertFallsBackToManualReview("Fraud detection service is currently unavailable", db);
    }

    // три кейса недоступности сервиса различаются настройкой мока и текстом причины,
    // остальной ответ у них общий — аварийная ветка бэка: перевод не проходит и уходит на ручную проверку.
    private void assertFallsBackToManualReview(String expectedFraudReasonPrefix, DbChecks db) {
        var transferRequest = new TransferWithFraudCheckRequest(
                senderAccountId,
                receiverAccountId,
                transferAmount
        );

        var transferResponse = SessionStorage.actAsUser().transferWithFraudCheck(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount()
        );

        ModelAssertions.assertThatModels(transferRequest, transferResponse).match();

        var expectedResponse = TransferWithFraudCheckResponse.builder()
                .status("MANUAL_REVIEW_REQUIRED")
                .message("Transfer requires manual review")
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(0.5)
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        softly.assertThat(transferResponse)
                .usingRecursiveComparison()
                .ignoringFields("transactionId", "fraudReason")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expectedResponse);

        softly.assertThat(((TransferWithFraudCheckResponse) transferResponse).getFraudReason())
                .startsWith(expectedFraudReasonPrefix);

        assertMoneyNotMoved(db);
    }

    // деньги списались у отправителя и пришли получателю, в БД лежит то же самое
    private void assertMoneyMoved(BigDecimal transferAmount, DbChecks db) {
        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser(2).getAccountByAccountNumber(receiverAccountNumber);

        softly.assertThat(senderAccount.getBalance())
                .isEqualByComparingTo(senderBalanceAfterSetup.subtract(transferAmount));
        softly.assertThat(receiverAccount.getBalance())
                .isEqualByComparingTo(receiverBalanceAfterSetup.add(transferAmount));

        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));
        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverAccountNumber));
    }

    // балансы обоих счетов остались такими же, как после сетапа, и БД это подтверждает
    private void assertMoneyNotMoved(DbChecks db) {
        var senderAccount = SessionStorage.actAsUser().getAccountByAccountNumber(senderAccountNumber);
        var receiverAccount = SessionStorage.actAsUser(2).getAccountByAccountNumber(receiverAccountNumber);

        softly.assertThat(senderAccount.getBalance()).isEqualByComparingTo(senderBalanceAfterSetup);
        softly.assertThat(receiverAccount.getBalance()).isEqualByComparingTo(receiverBalanceAfterSetup);

        db.assertMatches(senderAccount, () -> DataBaseSteps.getAccountByAccountNumber(senderAccountNumber));
        db.assertMatches(receiverAccount, () -> DataBaseSteps.getAccountByAccountNumber(receiverAccountNumber));
    }
}
