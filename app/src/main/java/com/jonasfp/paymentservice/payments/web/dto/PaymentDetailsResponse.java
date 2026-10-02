package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.jonasfp.paymentservice.domain.PaymentStatus;

public record PaymentDetailsResponse(
    UUID id, 
    String customerId, 
    UUID invoiceId,
    BigInteger authorizedAmount,
    BigInteger capturedAmount,
    BigInteger refundedAmount, 
    String currency, 
    PaymentStatus status,
    String processorReference,
    OffsetDateTime createdAt,
    List<PaymentEventResponse> history
) {
}
