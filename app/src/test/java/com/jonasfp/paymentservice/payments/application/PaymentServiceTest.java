package com.jonasfp.paymentservice.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonasfp.paymentservice.domain.PaymentStatus;
import com.jonasfp.paymentservice.payments.domain.IdempotencyKey;
import com.jonasfp.paymentservice.payments.domain.Payment;
import com.jonasfp.paymentservice.payments.domain.PaymentEvent;
import com.jonasfp.paymentservice.payments.web.dto.AuthorizePaymentRequest;
import com.jonasfp.paymentservice.payments.web.dto.PaymentResponse;
import com.jonasfp.paymentservice.payments.infra.CaptureRepository;
import com.jonasfp.paymentservice.payments.infra.IdempotencyKeyRepository;
import com.jonasfp.paymentservice.ledger.infra.JournalEntryRepository;
import com.jonasfp.paymentservice.ledger.infra.JournalLineRepository;
import com.jonasfp.paymentservice.ledger.infra.LedgerAccountRepository;
import com.jonasfp.paymentservice.payments.infra.PaymentEventRepository;
import com.jonasfp.paymentservice.payments.infra.PaymentRepository;
import com.jonasfp.paymentservice.payments.infra.RefundRepository;

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
            "customer-1", UUID.randomUUID(), new BigInteger("10000"),
            "USD");

        when(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(any(), any(),
                any())).thenReturn(Optional.empty());

        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
            .thenAnswer(invocation -> {
                IdempotencyKey entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> {
                Payment entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        when(paymentEventRepository.save(any(PaymentEvent.class)))
            .thenAnswer(invocation -> {
                PaymentEvent entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When
        PaymentResponse response = paymentService.authorize(idempotencyKey,
            request);

        // Then
        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.minorAmount()).isEqualTo(new BigInteger("10000"));
        assertThat(response.status()).isEqualTo(PaymentStatus.AUTHORIZED);

        verify(idempotencyKeyRepository, times(2))
            .save(any(IdempotencyKey.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(paymentEventRepository).save(any(PaymentEvent.class));
    }

    @Test
    void authorize_completedRequest_returnsCachedResponse()
        throws JsonProcessingException {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigInteger("10000"),
            "USD");
        String requestHash = calculateHash(request);

        PaymentResponse cachedResponse = new PaymentResponse(UUID.randomUUID(),
            "customer-1", request.invoiceId(), request.minorAmount(),
            request.currency(), PaymentStatus.AUTHORIZED, "proc_123");

        IdempotencyKey existingKey = new IdempotencyKey();
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
                BigInteger.class)
            .isEqualTo(cachedResponse);
        verify(paymentRepository, never()).save(any());
        verify(paymentEventRepository, never()).save(any());
    }

    @Test
    void authorize_startedRequest_throwsException() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigInteger("10000"),
            "USD");
        String requestHash = calculateHash(request);

        IdempotencyKey existingKey = new IdempotencyKey();
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
            "customer-1", UUID.randomUUID(), new BigInteger("10000"),
            "USD");

        IdempotencyKey existingKey = new IdempotencyKey();
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
