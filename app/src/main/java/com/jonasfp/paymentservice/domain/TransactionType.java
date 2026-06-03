package com.jonasfp.paymentservice.domain;

public enum TransactionType {
    CAPTURE, REFUND;

    public boolean isCapture() {
        return this == CAPTURE;
    }

    public boolean isRefund() {
        return this == REFUND;
    }
}
