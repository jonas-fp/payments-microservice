package com.jonasfp.paymentservice.domain;

public enum PaymentStatus {
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    FULLY_REFUNDED,
    VOIDED;

    public boolean canBeCaptured() {
        return this == AUTHORIZED;
    }

    public boolean canBeRefunded() {
        return this == CAPTURED || this == PARTIALLY_REFUNDED;
    }
}
