package com.payment_processing_system.payment_processing_system.payments.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CapturePaymentRequest(@NotBlank String customerId,
        @NotNull @Positive BigDecimal amountMinor, @NotBlank String currency) {
}
