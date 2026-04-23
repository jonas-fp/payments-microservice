package com.payment_processing_system.payment_processing_system.db;

import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
public class MigrationConstraintTests {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine").withDatabaseName("payments")
                    .withUsername("payments").withPassword("payments")
                    .withInitScript("init-db.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void paymentsStatusConstraintUsesFullyRefunded() {
        UUID paymentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO payments (
                    id, customer_id, invoice_id, authorized_amount,
                    captured_amount, refunded_amount, currency, status,
                    processor_payment_reference
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, paymentId, "customer-1", invoiceId,
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                "USD", "FULLY_REFUNDED", "processor-payment-1");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO payments (
                    id, customer_id, invoice_id, authorized_amount,
                    captured_amount, refunded_amount, currency, status,
                    processor_payment_reference
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), "customer-2", UUID.randomUUID(),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                "USD", "REFUNDED", "processor-payment-2"))
                        .isInstanceOf(DataIntegrityViolationException.class);
    }
}
