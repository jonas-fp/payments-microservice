package com.jonasfp.paymentservice.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public record Money(BigDecimal majorAmount, CurrencyCode currency) {

    public Money {
        majorAmount = Objects.requireNonNull(majorAmount, 
            "majorAmount must not be null");
        
        currency = Objects.requireNonNull(currency,
            "currency must not be null");
    }

    public static Money of(BigDecimal majorAmount, String currency) {
        return new Money(majorAmount, CurrencyCode.of(currency));
    }

    public static Money fromMinor(BigInteger minorAmount, String currency) {
        Objects.requireNonNull(minorAmount, "minorAmount must not be null");

        // NOTE: This only works for currencies with two decimal places
        BigDecimal majorAmount = new BigDecimal(minorAmount).movePointLeft(2);
        
        return new Money(majorAmount, CurrencyCode.of(currency));
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(majorAmount.add(other.majorAmount),
            currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(majorAmount.subtract(other.majorAmount),
            currency);
    }

    public boolean isZero() {
        return majorAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigInteger toMinor() {
        // NOTE: This only works for currencies with two decimal places
        return majorAmount.movePointRight(2).toBigInteger();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "money values must use the same currency");
        }
    }
}
