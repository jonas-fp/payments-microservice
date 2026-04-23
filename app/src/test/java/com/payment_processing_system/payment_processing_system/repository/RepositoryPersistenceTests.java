package com.payment_processing_system.payment_processing_system.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;
import com.payment_processing_system.payment_processing_system.entity.PaymentEntity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
public class RepositoryPersistenceTests {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine").withDatabaseName("payments")
                    .withUsername("payments").withPassword("payments")
                    .withInitScript("init-db.sql");

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void paymentRepositoryCanSaveAndLoadPayments() {
        PaymentEntity saved = paymentRepository.saveAndFlush(newPayment());

        PaymentEntity loaded = paymentRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(loaded.getCustomerId()).isEqualTo("customer-1");
        assertThat(loaded.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(loaded.getCurrency()).isEqualTo("USD");
    }

    private PaymentEntity newPayment() {
        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId("customer-1");
        payment.setInvoiceId(UUID.randomUUID());
        payment.setAuthorizedAmount(new BigDecimal("100.00"));
        payment.setCapturedAmount(BigDecimal.ZERO);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProcessorPaymentReference("processor-payment-1");
        return payment;
    }
}
