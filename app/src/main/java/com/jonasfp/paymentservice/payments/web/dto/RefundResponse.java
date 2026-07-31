package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigInteger;
import java.util.UUID;
import com.jonasfp.paymentservice.domain.PaymentStatus;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    BigInteger minorAmount,
    String currency,
    PaymentStatus status,
    String processorReference) {
}
