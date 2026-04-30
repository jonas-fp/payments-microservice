package com.payment_processing_system.payment_processing_system.payments.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CapturePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class PaymentControllerIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16-alpine").withDatabaseName("payments")
            .withUsername("payments").withPassword("payments")
            .withInitScript("init-db.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    void authorize_validRequest_returnsCreated() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("customer-1"))
            .andExpect(jsonPath("$.amountMinor").value(10000))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }

    @Test
    void authorize_idempotentRequest_returnsCreatedAndSameResponse()
        throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        // First request
        mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Second request (idempotent)
        mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("customer-1"))
            .andExpect(jsonPath("$.amountMinor").value(10000));
    }

    @Test
    void authorize_mismatchedRequestBody_returnsUnprocessableEntity()
        throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest request1 = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");
        AuthorizePaymentRequest request2 = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("20000"),
            "USD");

        // First request
        mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated());

        // Second request with different body
        mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void capture_validRequest_returnsCreated() throws Exception {
        // 1. Authorize a payment first
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"),
            "USD");

        String authResponseJson = mockMvc
            .perform(post("/v1/payments/authorize")
                .header("Idempotency-Key", authIdempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isCreated()).andReturn().getResponse()
            .getContentAsString();

        PaymentResponse authResponse = objectMapper.readValue(authResponseJson,
            PaymentResponse.class);
        UUID paymentId = authResponse.id();

        // 2. Capture the payment
        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        mockMvc.perform(post("/v1/payments/" + paymentId + "/capture")
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(capRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
            .andExpect(jsonPath("$.status").value("CAPTURED"))
            .andExpect(jsonPath("$.amountMinor").value(10000));
    }

    @Test
    void refund_validRequest_returnsCreated() throws Exception {
        // 1. Authorize a payment
        String authIdempotencyKey = UUID.randomUUID().toString();
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "customer-1", UUID.randomUUID(), new BigDecimal("10000"), "USD");

        String authResponseJson = mockMvc.perform(post("/v1/payments/authorize")
            .header("Idempotency-Key", authIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(authRequest)))
            .andExpect(status().isCreated()).andReturn().getResponse()
            .getContentAsString();

        entityManager.flush();
        entityManager.clear();

        UUID paymentId = objectMapper
            .readValue(authResponseJson, PaymentResponse.class).id();

        // 2. Capture the payment
        String capIdempotencyKey = UUID.randomUUID().toString();
        CapturePaymentRequest capRequest = new CapturePaymentRequest(
            "customer-1", new BigDecimal("10000"), "USD");

        mockMvc.perform(post("/v1/payments/" + paymentId + "/capture")
            .header("Idempotency-Key", capIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(capRequest)))
            .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();

        // 3. Refund the payment
        String refIdempotencyKey = UUID.randomUUID().toString();
        RefundRequest refRequest = new RefundRequest(
            "customer-1", new BigDecimal("5000"), "USD");

        mockMvc.perform(post("/v1/payments/" + paymentId + "/refunds")
            .header("Idempotency-Key", refIdempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
            .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
            .andExpect(jsonPath("$.amountMinor").value(5000));
    }
}
