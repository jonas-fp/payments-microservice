package com.payment_processing_system.payment_processing_system.payments.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;

public record PaymentResponse(UUID id, String customerId, UUID invoiceId,
        BigDecimal amountMinor, String currency, PaymentStatus status,
        String processorReference) {
}
