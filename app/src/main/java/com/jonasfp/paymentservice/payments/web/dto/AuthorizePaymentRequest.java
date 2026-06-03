package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AuthorizePaymentRequest(@NotBlank String customerId,
        @NotNull UUID invoiceId, @NotNull @Positive BigDecimal amountMinor,
        @NotBlank String currency) {
}
