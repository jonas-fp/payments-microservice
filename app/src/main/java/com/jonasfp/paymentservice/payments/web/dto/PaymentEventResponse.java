package com.jonasfp.paymentservice.payments.web.dto;

import com.jonasfp.paymentservice.domain.PaymentEventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentEventResponse(
    UUID id,
    PaymentEventType eventType,
    String processorEventReference,
    OffsetDateTime createdAt) {
}
