package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigInteger;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AuthorizePaymentRequest(@NotBlank String customerId,
        @NotNull UUID invoiceId, @NotNull @Positive BigInteger minorAmount,
        @NotBlank String currency) {
}
