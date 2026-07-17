package com.jonasfp.paymentservice.payments.web.dto;

import java.math.BigInteger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CapturePaymentRequest(@NotBlank String customerId,
        @NotNull @Positive BigInteger minorAmount, @NotBlank String currency) {
}
