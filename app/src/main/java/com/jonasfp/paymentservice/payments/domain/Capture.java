package com.jonasfp.paymentservice.payments.domain;

import java.math.BigDecimal;
import java.util.UUID;
import com.jonasfp.paymentservice.domain.Money;
import com.jonasfp.paymentservice.infra.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "captures")
public class Capture extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "payment_event_id", nullable = false)
    private UUID paymentEventId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "processor_capture_reference", length = 128)
    private String processorCaptureReference;

    public Capture() {
    }

    public Money getMoney() {
        return Money.of(amount, currency);
    }

    public void setMoney(Money money) {
        this.amount = money.majorAmount();
        this.currency = money.currency().value();
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public UUID getPaymentEventId() {
        return paymentEventId;
    }

    public void setPaymentEventId(UUID paymentEventId) {
        this.paymentEventId = paymentEventId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProcessorCaptureReference() {
        return processorCaptureReference;
    }

    public void setProcessorCaptureReference(String processorCaptureReference) {
        this.processorCaptureReference = processorCaptureReference;
    }
}
