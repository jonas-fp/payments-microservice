package com.payment_processing_system.payment_processing_system.payments.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

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
                                        .withUsername("payments")
                                        .withPassword("payments")
                                        .withInitScript("init-db.sql");

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void authorize_validRequest_returnsCreated() throws Exception {
                String idempotencyKey = UUID.randomUUID().toString();
                AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                                "customer-1", UUID.randomUUID(),
                                new BigDecimal("10000"), "USD");

                mockMvc.perform(post("/v1/payments/authorize")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.customerId")
                                                .value("customer-1"))
                                .andExpect(jsonPath("$.amountMinor")
                                                .value(10000))
                                .andExpect(jsonPath("$.currency").value("USD"))
                                .andExpect(jsonPath("$.status")
                                                .value("AUTHORIZED"));
        }

        @Test
        void authorize_idempotentRequest_returnsCreatedAndSameResponse()
                        throws Exception {
                String idempotencyKey = UUID.randomUUID().toString();
                AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                                "customer-1", UUID.randomUUID(),
                                new BigDecimal("10000"), "USD");

                // First request
                mockMvc.perform(post("/v1/payments/authorize")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Second request (idempotent)
                mockMvc.perform(post("/v1/payments/authorize")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.customerId")
                                                .value("customer-1"))
                                .andExpect(jsonPath("$.amountMinor")
                                                .value(10000));
        }

        @Test
        void authorize_mismatchedRequestBody_returnsUnprocessableEntity()
                        throws Exception {
                String idempotencyKey = UUID.randomUUID().toString();
                AuthorizePaymentRequest request1 = new AuthorizePaymentRequest(
                                "customer-1", UUID.randomUUID(),
                                new BigDecimal("10000"), "USD");
                AuthorizePaymentRequest request2 = new AuthorizePaymentRequest(
                                "customer-1", UUID.randomUUID(),
                                new BigDecimal("20000"), "USD");

                // First request
                mockMvc.perform(post("/v1/payments/authorize")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(request1)))
                                .andExpect(status().isCreated());

                // Second request with different body
                mockMvc.perform(post("/v1/payments/authorize")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(request2)))
                                .andExpect(status().isUnprocessableEntity());
        }
}
