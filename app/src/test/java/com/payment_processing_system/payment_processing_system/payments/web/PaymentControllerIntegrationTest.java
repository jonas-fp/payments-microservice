package com.payment_processing_system.payment_processing_system.payments.web;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.assertThat;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CapturePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CaptureResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundResponse;
import com.payment_processing_system.payment_processing_system.repository.CaptureRepository;
import com.payment_processing_system.payment_processing_system.repository.IdempotencyKeyRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentRepository;
import com.payment_processing_system.payment_processing_system.repository.RefundRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class PaymentControllerIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine").withDatabaseName("payments")
            .withUsername("payments").withPassword("payments")
            .withInitScript("init-db.sql");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private CaptureRepository captureRepository;

    @Autowired
    private RefundRepository refundRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                "TRUNCATE TABLE journal_lines, journal_entries, captures, " +
                    "refunds, payment_events, payments, idempotency_keys " +
                    "RESTART IDENTITY CASCADE")
                .executeUpdate();
            return null;
        });
    }

    @Test
    void authorize_validRequest_returnsCreated() {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.customerId").isEqualTo("customer-1")
            .jsonPath("$.amountMinor").isEqualTo(10000)
            .jsonPath("$.currency").isEqualTo("USD")
            .jsonPath("$.status").isEqualTo("AUTHORIZED");
    }

    @Test
    void authorize_idempotentRequest_returnsCreatedAndSameResponse() {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        // First request
        PaymentResponse firstResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        // Second request (idempotent)
        PaymentResponse secondResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        // Response assertions
        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.customerId())
            .isEqualTo(firstResponse.customerId());
        assertThat(secondResponse.amountMinor())
            .isEqualTo(firstResponse.amountMinor());

        // DB assertions
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void authorize_mismatchedRequestBody_returnsUnprocessableEntity() {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request1 = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");
        AuthorizePaymentRequest request2 = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("20000"),
            "USD");

        // First request
        webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange()
            .expectStatus().isCreated();

        // Second request with different body
        webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(422); // Unprocessable Entity
    }

    @Test
    void capture_validRequest_returnsCreated() {
        // 1. Authorize a payment first
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        // 2. Capture the payment
        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.paymentId").isEqualTo(paymentId.toString())
            .jsonPath("$.status").isEqualTo("CAPTURED")
            .jsonPath("$.amountMinor").isEqualTo(10000);
    }

    @Test
    void capture_idempotentRequest_returnsCreatedAndSameResponse() {
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        CaptureResponse firstResponse = webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CaptureResponse.class)
            .returnResult()
            .getResponseBody();

        CaptureResponse secondResponse = webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(CaptureResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.paymentId())
            .isEqualTo(firstResponse.paymentId());
        assertThat(secondResponse.amountMinor())
            .isEqualTo(firstResponse.amountMinor());
        assertThat(secondResponse.currency())
            .isEqualTo(firstResponse.currency());
        assertThat(secondResponse.status()).isEqualTo(firstResponse.status());
        assertThat(secondResponse.processorReference())
            .isEqualTo(firstResponse.processorReference());

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(captureRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                "customer-1", capIdempotencyKey, "CAPTURE"))
                    .isPresent();
    }

    @Test
    void capture_mismatchedRequestBody_returnsUnprocessableEntity() {
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest request1 = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");
        CapturePaymentRequest request2 = new CapturePaymentRequest(
            "customer-1", new BigDecimal("20000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange()
            .expectStatus().isCreated();

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(422);
    }

    @Test
    void refund_validRequest_returnsCreated() {
        // 1. Authorize a payment
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"), "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        // 2. Capture the payment
        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated();

        // 3. Refund the payment
        String refIdempotencyKey = UUID.randomUUID().toString();
        RefundRequest refRequest = new RefundRequest(
            "customer-1", new BigDecimal("5000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/refunds", paymentId)
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.paymentId").isEqualTo(paymentId.toString())
            .jsonPath("$.status").isEqualTo("PARTIALLY_REFUNDED")
            .jsonPath("$.amountMinor").isEqualTo(5000);
    }

    @Test
    void refund_idempotentRequest_returnsCreatedAndSameResponse() {
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"), "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated();

        String refIdempotencyKey = UUID.randomUUID().toString();
        RefundRequest refRequest = new RefundRequest(
            "customer-1", new BigDecimal("5000"), "USD");

        RefundResponse firstResponse = webTestClient.post()
            .uri("/v1/payments/{id}/refunds", paymentId)
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RefundResponse.class)
            .returnResult()
            .getResponseBody();

        RefundResponse secondResponse = webTestClient.post()
            .uri("/v1/payments/{id}/refunds", paymentId)
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RefundResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
        assertThat(secondResponse.paymentId())
            .isEqualTo(firstResponse.paymentId());
        assertThat(secondResponse.amountMinor())
            .isEqualTo(firstResponse.amountMinor());
        assertThat(secondResponse.currency())
            .isEqualTo(firstResponse.currency());
        assertThat(secondResponse.status()).isEqualTo(firstResponse.status());
        assertThat(secondResponse.processorReference())
            .isEqualTo(firstResponse.processorReference());

        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(captureRepository.count()).isEqualTo(1);
        assertThat(refundRepository.count()).isEqualTo(1);
        assertThat(idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                "customer-1", refIdempotencyKey, "REFUND"))
                    .isPresent();
    }

    @Test
    void refund_mismatchedRequestBody_returnsUnprocessableEntity() {
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"), "USD");

        PaymentResponse authResponse = webTestClient.post()
            .uri("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(PaymentResponse.class)
            .returnResult()
            .getResponseBody();

        UUID paymentId = authResponse.id();

        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/capture", paymentId)
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(capRequest)
            .exchange()
            .expectStatus().isCreated();

        String refIdempotencyKey = UUID.randomUUID().toString();
        RefundRequest request1 = new RefundRequest(
            "customer-1", new BigDecimal("5000"), "USD");
        RefundRequest request2 = new RefundRequest(
            "customer-1", new BigDecimal("7000"), "USD");

        webTestClient.post()
            .uri("/v1/payments/{id}/refunds", paymentId)
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange()
            .expectStatus().isCreated();

        webTestClient.post()
            .uri("/v1/payments/{id}/refunds", paymentId)
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isEqualTo(422);
    }
}
