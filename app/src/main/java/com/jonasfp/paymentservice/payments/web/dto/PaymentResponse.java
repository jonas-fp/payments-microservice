package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigDecimal;
import java.util.UUID;
import com.jonasfp.paymentservice.domain.PaymentStatus;

public record PaymentResponse(UUID id, String customerId, UUID invoiceId,
        BigDecimal amountMinor, String currency, PaymentStatus status,
        String processorReference) {
}
