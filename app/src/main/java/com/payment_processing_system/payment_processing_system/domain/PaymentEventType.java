package com.payment_processing_system.payment_processing_system.domain;

public enum PaymentEventType {
    AUTHORIZE_REQUESTED, AUTHORIZE_SUCCESS, AUTHORIZE_FAILED, AUTHORIZE_VOIDED,
    CAPTURE_REQUESTED, CAPTURE_SUCCESS, CAPTURE_FAILED,
    CAPTURE_VOIDED,
    REFUND_REQUESTED, REFUND_SUCCESS, REFUND_FAILED,
    REFUND_VOIDED;

    public boolean createsJournalEntry() {
        return this == CAPTURE_SUCCESS || this == REFUND_SUCCESS;
    }
}
