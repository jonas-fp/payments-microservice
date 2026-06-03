package com.jonasfp.paymentservice.payments.infra;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.jonasfp.paymentservice.domain.PaymentStatus;
import com.jonasfp.paymentservice.payments.domain.Payment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
public class PaymentRepositoryTest {

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
        Payment saved = paymentRepository.saveAndFlush(newPayment());

        Payment loaded = paymentRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(loaded.getCustomerId()).isEqualTo("customer-1");
        assertThat(loaded.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(loaded.getCurrency()).isEqualTo("USD");
    }

    private Payment newPayment() {
        Payment payment = new Payment();
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
