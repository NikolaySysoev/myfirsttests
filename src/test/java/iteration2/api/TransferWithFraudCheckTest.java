package iteration2.api;

import api.dao.checks.DbChecks;
import api.models.assertions.ModelAssertions;
import api.models.v2.FraudFallbackReason;
import api.models.v2.TransferStatus;
import api.models.v2.requests.TransferWithFraudCheckRequest;
import api.models.v2.responses.TransferWithFraudCheckResponse;
import api.requests.steps.DataBaseSteps;
import common.annotations.ApiVersion;
import common.annotations.FraudCheckMock;
import common.annotations.UserSession;
import common.extensions.FraudCheckWireMockExtension;
import common.mocks.fraud.FraudReason;
import common.mocks.fraud.FraudRiskScore;
import common.storage.SessionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static api.configs.BackendVersion.V2;
import static common.annotations.FraudCheckMock.Behaviour.CONNECTION_ERROR;
import static common.annotations.FraudCheckMock.Behaviour.HTTP_ERROR;
import static common.annotations.FraudCheckMock.Behaviour.SERVICE_DOWN;
import static common.mocks.fraud.FraudCheckStatus.SUCCESS;
import static common.mocks.fraud.FraudDecision.APPROVED;
import static common.mocks.fraud.FraudDecision.BLOCKED;
import static common.mocks.fraud.FraudDecision.REVIEW_REQUIRED;
import static common.mocks.fraud.FraudDecision.VERIFICATION_REQUIRED;

@ApiVersion(V2)
@ExtendWith(FraudCheckWireMockExtension.class)
// мок фрод-сервиса один на всех — фиксированный порт 8089, его же знает бэк.
// При параллельном прогоне тесты с моком идут строго по очереди, остальные — параллельно.
@ResourceLock(FraudCheckMock.RESOURCE_LOCK)
public class TransferWithFraudCheckTest extends BaseApiTest {
    private static final int FRAUD_SERVICE_ERROR_STATUS = 500;
    private static final double FALLBACK_RISK_SCORE = 0.5;
    private static final String TRANSACTION_ID_FIELD = "transactionId";
    private static final String FRAUD_REASON_FIELD = "fraudReason";
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
            status = SUCCESS,
            decision = APPROVED,
            riskScore = FraudRiskScore.LOW_RISK,
            reason = FraudReason.LOW_RISK,
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
                .status(TransferStatus.APPROVED.getStatus())
                .message(TransferStatus.APPROVED.getMessage())
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(FraudRiskScore.LOW_RISK)
                .fraudReason(FraudReason.LOW_RISK.getText())
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        assertMatchesExpected(transferResponse, expectedResponse, TRANSACTION_ID_FIELD);

        assertMoneyMoved(transferAmount, db);
    }

    @Test
    @UserSession(2)
    @DisplayName("BLOCKED: перевод заблокирован, деньги не двигаются")
    @FraudCheckMock(
            status = SUCCESS,
            decision = BLOCKED,
            riskScore = FraudRiskScore.HIGH_RISK,
            reason = FraudReason.HIGH_RISK,
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
                .status(TransferStatus.BLOCKED.getStatus())
                .message(TransferStatus.BLOCKED.getMessage())
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(FraudRiskScore.HIGH_RISK)
                .fraudReason(FraudReason.HIGH_RISK.getText())
                .requiresManualReview(false)
                .requiresVerification(false)
                .build();

        assertMatchesExpected(transferResponse, expectedResponse, TRANSACTION_ID_FIELD);

        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("REVIEW_REQUIRED: перевод уходит на ручную проверку")
    @FraudCheckMock(
            status = SUCCESS,
            decision = REVIEW_REQUIRED,
            riskScore = FraudRiskScore.SUSPICIOUS_ACTIVITY,
            reason = FraudReason.SUSPICIOUS_ACTIVITY,
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
                .status(TransferStatus.MANUAL_REVIEW_REQUIRED.getStatus())
                .message(TransferStatus.MANUAL_REVIEW_REQUIRED.getMessage())
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(FraudRiskScore.SUSPICIOUS_ACTIVITY)
                .fraudReason(FraudReason.SUSPICIOUS_ACTIVITY.getText())
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        assertMatchesExpected(transferResponse, expectedResponse, TRANSACTION_ID_FIELD);

        // до ручной проверки деньги не переводятся
        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("VERIFICATION_REQUIRED: требуется дополнительное подтверждение")
    @FraudCheckMock(
            status = SUCCESS,
            decision = VERIFICATION_REQUIRED,
            riskScore = FraudRiskScore.VERIFICATION_NEEDED,
            reason = FraudReason.VERIFICATION_NEEDED,
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
                .status(TransferStatus.VERIFICATION_REQUIRED.getStatus())
                .message(TransferStatus.VERIFICATION_REQUIRED.getMessage())
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(FraudRiskScore.VERIFICATION_NEEDED)
                .fraudReason(FraudReason.VERIFICATION_NEEDED.getText())
                .requiresManualReview(false)
                .requiresVerification(true)
                .build();

        assertMatchesExpected(transferResponse, expectedResponse, TRANSACTION_ID_FIELD);

        // до подтверждения деньги не переводятся
        assertMoneyNotMoved(db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис отвечает 500: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = HTTP_ERROR, httpStatus = FRAUD_SERVICE_ERROR_STATUS)
    public void transferFallsBackToManualReviewWhenFraudServiceReturnsError(DbChecks db) {
        assertFallsBackToManualReview(FraudFallbackReason.UNEXPECTED_ERROR.withHttpStatus(FRAUD_SERVICE_ERROR_STATUS), db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис рвёт соединение: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = CONNECTION_ERROR)
    public void transferFallsBackToManualReviewWhenConnectionIsReset(DbChecks db) {
        assertFallsBackToManualReview(FraudFallbackReason.SERVICE_UNAVAILABLE.getText(), db);
    }

    @Test
    @UserSession(2)
    @DisplayName("Фрод-сервис не поднят: бэк уходит в ручную проверку")
    @FraudCheckMock(behaviour = SERVICE_DOWN)
    public void transferFallsBackToManualReviewWhenServiceIsDown(DbChecks db) {
        assertFallsBackToManualReview(FraudFallbackReason.SERVICE_UNAVAILABLE.getText(), db);
    }


    // ========= HELPERS =========

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
                .status(TransferStatus.MANUAL_REVIEW_REQUIRED.getStatus())
                .message(TransferStatus.MANUAL_REVIEW_REQUIRED.getMessage())
                .amount(transferAmount)
                .senderAccountId(senderAccountId)
                .receiverAccountId(receiverAccountId)
                .fraudRiskScore(FALLBACK_RISK_SCORE)
                .requiresManualReview(true)
                .requiresVerification(false)
                .build();

        assertMatchesExpected(transferResponse, expectedResponse, TRANSACTION_ID_FIELD, FRAUD_REASON_FIELD);

        softly.assertThat(((TransferWithFraudCheckResponse) transferResponse).getFraudReason())
                .startsWith(expectedFraudReasonPrefix);

        assertMoneyNotMoved(db);
    }
    
    // результат фрод-проверки: сверяем ответ целиком с эталоном.
    // transactionId генерирует бэк, в эталоне его нет — его передают в ignoredFields
    // (см. TRANSACTION_ID_FIELD).
    // BigDecimal.equals учитывает scale (2141.13 != 2141.130), поэтому
    // для этого типа сравниваем через compareTo.
    private void assertMatchesExpected(Object actual, TransferWithFraudCheckResponse expected,
                                       String... ignoredFields) {
        softly.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields(ignoredFields)
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
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
