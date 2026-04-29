package com.payment_processing_system.payment_processing_system.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;
import com.payment_processing_system.payment_processing_system.entity.IdempotencyKeyEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEventEntity;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.repository.CaptureRepository;
import com.payment_processing_system.payment_processing_system.repository.IdempotencyKeyRepository;
import com.payment_processing_system.payment_processing_system.repository.JournalEntryRepository;
import com.payment_processing_system.payment_processing_system.repository.JournalLineRepository;
import com.payment_processing_system.payment_processing_system.repository.LedgerAccountRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentEventRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentRepository;
import com.payment_processing_system.payment_processing_system.repository.RefundRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventRepository paymentEventRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private CaptureRepository captureRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalLineRepository journalLineRepository;
    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    private ObjectMapper objectMapper = new ObjectMapper();
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository,
                paymentEventRepository, idempotencyKeyRepository,
                captureRepository, refundRepository, journalEntryRepository,
                journalLineRepository, ledgerAccountRepository, objectMapper);
    }

    @Test
    void authorize_newRequest_createsPaymentAndEvents() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
                "USD");

        when(idempotencyKeyRepository
                .findByCustomerIdAndIdempotencyKeyAndActionType(any(), any(),
                        any())).thenReturn(Optional.empty());

        when(idempotencyKeyRepository.save(any(IdempotencyKeyEntity.class)))
                .thenAnswer(invocation -> {
                    IdempotencyKeyEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        when(paymentEventRepository.save(any(PaymentEventEntity.class)))
                .thenAnswer(invocation -> {
                    PaymentEventEntity entity = invocation.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    return entity;
                });

        // When
        PaymentResponse response = paymentService.authorize(idempotencyKey,
                request);

        // Then
        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.amountMinor()).isEqualTo(new BigDecimal("10000"));
        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);

        verify(idempotencyKeyRepository, times(2))
                .save(any(IdempotencyKeyEntity.class));
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(paymentEventRepository).save(any(PaymentEventEntity.class));
    }

    @Test
    void authorize_completedRequest_returnsCachedResponse()
            throws JsonProcessingException {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
                "USD");
        String requestHash = calculateHash(request);

        PaymentResponse cachedResponse = new PaymentResponse(UUID.randomUUID(),
                "customer-1", request.invoiceId(), request.amountMinor(),
                request.currency(), PaymentStatus.AUTHORIZED, "proc_123");

        IdempotencyKeyEntity existingKey = new IdempotencyKeyEntity();
        existingKey.setCustomerId("customer-1");
        existingKey.setIdempotencyKey(idempotencyKey);
        existingKey.setActionType("AUTHORIZE");
        existingKey.setRequestHash(requestHash);
        existingKey.setResponseStatus("COMPLETED");
        existingKey.setResponseBody(objectMapper.valueToTree(cachedResponse));

        when(idempotencyKeyRepository
                .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                        idempotencyKey, "AUTHORIZE"))
                                .thenReturn(Optional.of(existingKey));

        // When
        PaymentResponse response = paymentService.authorize(idempotencyKey,
                request);

        // Then
        assertThat(response).usingRecursiveComparison()
                .withEqualsForType((b1, b2) -> b1.compareTo(b2) == 0,
                        BigDecimal.class)
                .isEqualTo(cachedResponse);
        verify(paymentRepository, never()).save(any());
        verify(paymentEventRepository, never()).save(any());
    }

    @Test
    void authorize_startedRequest_throwsException() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
                "USD");
        String requestHash = calculateHash(request);

        IdempotencyKeyEntity existingKey = new IdempotencyKeyEntity();
        existingKey.setCustomerId("customer-1");
        existingKey.setIdempotencyKey(idempotencyKey);
        existingKey.setActionType("AUTHORIZE");
        existingKey.setRequestHash(requestHash);
        existingKey.setResponseStatus("STARTED");

        when(idempotencyKeyRepository
                .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                        idempotencyKey, "AUTHORIZE"))
                                .thenReturn(Optional.of(existingKey));

        // When / Then
        assertThatThrownBy(
                () -> paymentService.authorize(idempotencyKey, request))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("already in progress");
    }

    @Test
    void authorize_differentRequestBody_throwsException() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
                "USD");

        IdempotencyKeyEntity existingKey = new IdempotencyKeyEntity();
        existingKey.setCustomerId("customer-1");
        existingKey.setIdempotencyKey(idempotencyKey);
        existingKey.setActionType("AUTHORIZE");
        existingKey.setRequestHash("different-hash");

        when(idempotencyKeyRepository
                .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                        idempotencyKey, "AUTHORIZE"))
                                .thenReturn(Optional.of(existingKey));

        // When / Then
        assertThatThrownBy(
                () -> paymentService.authorize(idempotencyKey, request))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(
                                "Idempotency key reuse with different request"
                                        + " body");
    }

    private String calculateHash(AuthorizePaymentRequest request) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA-256");
            String json = objectMapper.writeValueAsString(request);
            byte[] hashBytes = digest.digest(
                    json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashBytes);
        } catch (java.security.NoSuchAlgorithmException
                | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
