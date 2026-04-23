package com.payment_processing_system.payment_processing_system.domain;

import java.util.Locale;
import java.util.Objects;

public record CurrencyCode(String value) {

    public CurrencyCode {
        value = Objects.requireNonNull(value, "value must not be null").trim()
                .toUpperCase(Locale.ROOT);
        if (value.length() != 3) {
            throw new IllegalArgumentException(
                    "currency code must be a 3-letter ISO 4217 code");
        }
        if (!value.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException(
                    "currency code must contain only letters");
        }
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
