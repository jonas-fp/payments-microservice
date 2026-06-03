package com.jonasfp.paymentservice.payments.domain;

import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.jonasfp.paymentservice.domain.PaymentEventType;
import com.jonasfp.paymentservice.infra.persistence.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_events")
public class PaymentEvent extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private PaymentEventType eventType;

    @Column(name = "processor_event_reference", length = 128)
    private String processorEventReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "processor_response", columnDefinition = "jsonb")
    private JsonNode processorResponse;

    @Column(name = "idempotency_key_id")
    private UUID idempotencyKeyId;

    public PaymentEvent() {
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public void setEventType(PaymentEventType eventType) {
        this.eventType = eventType;
    }

    public String getProcessorEventReference() {
        return processorEventReference;
    }

    public void setProcessorEventReference(String processorEventReference) {
        this.processorEventReference = processorEventReference;
    }

    public UUID getIdempotencyKeyId() {
        return idempotencyKeyId;
    }

    public void setIdempotencyKeyId(UUID idempotencyKeyId) {
        this.idempotencyKeyId = idempotencyKeyId;
    }
}
