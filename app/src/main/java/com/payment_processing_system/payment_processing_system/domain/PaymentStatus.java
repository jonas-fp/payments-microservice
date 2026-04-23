package com.payment_processing_system.payment_processing_system.domain;

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
