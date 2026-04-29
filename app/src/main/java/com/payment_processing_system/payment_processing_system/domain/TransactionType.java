package com.payment_processing_system.payment_processing_system.domain;

public enum TransactionType {
    CAPTURE, REFUND;

    public boolean isCapture() {
        return this == CAPTURE;
    }

    public boolean isRefund() {
        return this == REFUND;
    }
}
