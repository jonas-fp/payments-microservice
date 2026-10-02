package com.jonasfp.paymentservice.payments.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonasfp.paymentservice.domain.PaymentEventType;
import com.jonasfp.paymentservice.domain.PaymentStatus;
import com.jonasfp.paymentservice.payments.domain.IdempotencyActionType;
import com.jonasfp.paymentservice.payments.domain.IdempotencyKey;
import com.jonasfp.paymentservice.payments.domain.IdempotencyResponseStatus;
import com.jonasfp.paymentservice.payments.domain.Payment;
import com.jonasfp.paymentservice.payments.domain.PaymentEvent;
import com.jonasfp.paymentservice.payments.web.dto.AuthorizePaymentRequest;
import com.jonasfp.paymentservice.payments.web.dto.PaymentDetailsResponse;
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
        existingKey.setActionType(IdempotencyActionType.AUTHORIZE);
        existingKey.setRequestHash(requestHash);
        existingKey.setResponseStatus(IdempotencyResponseStatus.COMPLETED);
        existingKey.setResponseBody(objectMapper.valueToTree(cachedResponse));

        when(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                idempotencyKey, IdempotencyActionType.AUTHORIZE))
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
        existingKey.setActionType(IdempotencyActionType.AUTHORIZE);
        existingKey.setRequestHash(requestHash);
        existingKey.setResponseStatus(IdempotencyResponseStatus.STARTED);

        when(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                idempotencyKey, IdempotencyActionType.AUTHORIZE))
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
        existingKey.setActionType(IdempotencyActionType.AUTHORIZE);
        existingKey.setRequestHash("different-hash");

        when(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType("customer-1",
                idempotencyKey, IdempotencyActionType.AUTHORIZE))
                    .thenReturn(Optional.of(existingKey));

        // When / Then
        assertThatThrownBy(
            () -> paymentService.authorize(idempotencyKey, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                    "Idempotency key reuse with different request"
                        + " body");
    }

    @Test
    void getPaymentDetailsById_existingPayment_returnsPaymentDetails() {
        // Given
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        OffsetDateTime paymentCreatedAt =
            OffsetDateTime.of(2026, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);

        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setCustomerId("customer-1");
        payment.setInvoiceId(invoiceId);
        payment.setAuthorizedAmount(new BigDecimal("100.00"));
        payment.setCapturedAmount(new BigDecimal("100.00"));
        payment.setRefundedAmount(new BigDecimal("25.00"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setProcessorPaymentReference("proc_payment_123");
        payment.setCreatedAt(paymentCreatedAt);

        PaymentEvent event1 = new PaymentEvent();
        event1.setId(UUID.randomUUID());
        event1.setPaymentId(paymentId);
        event1.setEventType(PaymentEventType.AUTHORIZE_SUCCESS);
        event1.setProcessorEventReference("proc_evt_1");
        event1.setCreatedAt(paymentCreatedAt);

        PaymentEvent event2 = new PaymentEvent();
        event2.setId(UUID.randomUUID());
        event2.setPaymentId(paymentId);
        event2.setEventType(PaymentEventType.CAPTURE_SUCCESS);
        event2.setProcessorEventReference("proc_evt_2");
        event2.setCreatedAt(paymentCreatedAt.plusMinutes(5));

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(paymentEventRepository
            .findByPaymentIdOrderByCreatedAtAsc(paymentId))
                .thenReturn(List.of(event1, event2));

        // When
        PaymentDetailsResponse response =
            paymentService.getPaymentDetailsById(paymentId);

        // Then
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.customerId()).isEqualTo("customer-1");
        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.authorizedAmount())
            .isEqualTo(BigInteger.valueOf(10000));
        assertThat(response.capturedAmount())
            .isEqualTo(BigInteger.valueOf(10000));
        assertThat(response.refundedAmount())
            .isEqualTo(BigInteger.valueOf(2500));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(response.processorReference())
            .isEqualTo("proc_payment_123");
        assertThat(response.createdAt()).isEqualTo(paymentCreatedAt);
        assertThat(response.history()).hasSize(2);
        assertThat(response.history().get(0).eventType())
            .isEqualTo(PaymentEventType.AUTHORIZE_SUCCESS);
        assertThat(response.history().get(0).processorEventReference())
            .isEqualTo("proc_evt_1");
        assertThat(response.history().get(1).eventType())
            .isEqualTo(PaymentEventType.CAPTURE_SUCCESS);
        assertThat(response.history().get(1).processorEventReference())
            .isEqualTo("proc_evt_2");

        verify(paymentRepository).findById(paymentId);
        verify(paymentEventRepository)
            .findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }

    @Test
    void getPaymentDetailsById_paymentNotFound_throwsException() {
        // Given
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(
            () -> paymentService.getPaymentDetailsById(paymentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No payment found");

        verify(paymentRepository).findById(paymentId);
        verify(paymentEventRepository, never())
            .findByPaymentIdOrderByCreatedAtAsc(any());
    }

    @Test
    void getPaymentDetailsById_paymentWithNoEvents_returnsDetailsWithEmptyHistory() {
        // Given
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setCustomerId("customer-1");
        payment.setInvoiceId(UUID.randomUUID());
        payment.setAuthorizedAmount(new BigDecimal("50.00"));
        payment.setCapturedAmount(BigDecimal.ZERO);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProcessorPaymentReference("proc_payment_456");

        when(paymentRepository.findById(paymentId))
            .thenReturn(Optional.of(payment));
        when(paymentEventRepository
            .findByPaymentIdOrderByCreatedAtAsc(paymentId))
                .thenReturn(List.of());

        // When
        PaymentDetailsResponse response =
            paymentService.getPaymentDetailsById(paymentId);

        // Then
        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.authorizedAmount())
            .isEqualTo(BigInteger.valueOf(5000));
        assertThat(response.history()).isEmpty();
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
