package com.payment_processing_system.payment_processing_system.entity;

import java.math.BigDecimal;

import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class PaymentEntity extends BaseEntity {

    @Column(name = "customer_id", nullable = false, length = 128)
    private String customerId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "authorized_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal capturedAmount;

    @Column(name = "refunded_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "processor_payment_reference", length = 128)
    private String processorPaymentReference;

    public PaymentEntity() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getAuthorizedAmount() {
        return authorizedAmount;
    }

    public void setAuthorizedAmount(BigDecimal authorizedAmount) {
        this.authorizedAmount = authorizedAmount;
    }

    public BigDecimal getCapturedAmount() {
        return capturedAmount;
    }

    public void setCapturedAmount(BigDecimal capturedAmount) {
        this.capturedAmount = capturedAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getProcessorPaymentReference() {
        return processorPaymentReference;
    }

    public void setProcessorPaymentReference(String processorPaymentReference) {
        this.processorPaymentReference = processorPaymentReference;
    }
}
