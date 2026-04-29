package com.payment_processing_system.payment_processing_system.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "captures")
public class CaptureEntity extends BaseEntity {

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

    public CaptureEntity() {
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
