package com.payment_processing_system.payment_processing_system.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amountMinor, CurrencyCode currency) {

    public Money {
        if (amountMinor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "amountMinor must be non-negative");
        }
        currency = Objects.requireNonNull(currency,
                "currency must not be null");
    }

    public static Money of(BigDecimal amountMinor, String currency) {
        return new Money(amountMinor, CurrencyCode.of(currency));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amountMinor.add(other.amountMinor),
                currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        BigDecimal result = amountMinor.subtract(other.amountMinor);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "resulting amountMinor must be non-negative");
        }
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amountMinor.compareTo(BigDecimal.ZERO) == 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "money values must use the same currency");
        }
    }
}
