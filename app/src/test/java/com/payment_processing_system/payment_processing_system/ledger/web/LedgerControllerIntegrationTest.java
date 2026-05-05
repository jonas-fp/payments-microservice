package com.payment_processing_system.payment_processing_system.ledger.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.payment_processing_system.payment_processing_system.entity.LedgerAccountEntity;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CapturePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.repository.LedgerAccountRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class LedgerControllerIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments")
            .withUsername("payments")
            .withPassword("payments")
            .withInitScript("init-db.sql");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    void getAccountBalance_afterCapture_returnsCorrectBalance() {
        // 1. Get the Cash Clearing account (Asset)
        LedgerAccountEntity cashClearing =
            ledgerAccountRepository.findByAccountCode("10001")
                .orElseThrow();
        UUID accountId = cashClearing.getId();

        // 2. Authorize a payment of 100.00
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

        // 3. Capture the payment
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

        // 4. Get balance
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/payments/subledger/accounts/{accountId}/balance")
                .queryParam("asOf", OffsetDateTime.now().toString())
                .build(accountId))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.accountId").isEqualTo(accountId.toString())
            .jsonPath("$.accountCode").isEqualTo("10001")
            .jsonPath("$.balance").isEqualTo(100.00)
            .jsonPath("$.currency").isEqualTo("USD");
    }

    @Test
    void getTrialBalance_afterCapture_returnsBalancedTrialBalance() {
        // 1. Authorize and Capture a payment of 100.00
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

        // 2. Get trial balance
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/payments/subledger/trial-balance")
                .queryParam("asOf", OffsetDateTime.now().toString())
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.entries[?(@.accountCode=='10001')].totalDebit")
            .isEqualTo(100.00)
            .jsonPath("$.entries[?(@.accountCode=='10001')].totalCredit")
            .isEqualTo(0)
            .jsonPath("$.entries[?(@.accountCode=='20002')].totalDebit")
            .isEqualTo(0)
            .jsonPath("$.entries[?(@.accountCode=='20002')].totalCredit")
            .isEqualTo(100.00);
    }
}
