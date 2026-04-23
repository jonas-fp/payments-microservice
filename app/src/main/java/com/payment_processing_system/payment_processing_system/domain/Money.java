package com.payment_processing_system.payment_processing_system.domain;

import java.util.Objects;

public record Money(long amountMinor, CurrencyCode currency) {

    public Money {
        if (amountMinor < 0) {
            throw new IllegalArgumentException(
                    "amountMinor must be non-negative");
        }
        currency = Objects.requireNonNull(currency,
                "currency must not be null");
    }

    public static Money of(long amountMinor, String currency) {
        return new Money(amountMinor, CurrencyCode.of(currency));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(amountMinor, other.amountMinor),
                currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        long result = Math.subtractExact(amountMinor, other.amountMinor);
        if (result < 0) {
            throw new IllegalArgumentException(
                    "resulting amountMinor must be non-negative");
        }
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amountMinor == 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "money values must use the same currency");
        }
    }
}
