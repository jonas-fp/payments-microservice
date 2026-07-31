package com.jonasfp.paymentservice.reconciliation.domain;

public enum ReconciliationBreakType {
    MISSING_INTERNAL_RECORD, MISSING_PROCESSOR_RECORD, 
    DUPLICATE_INTERNAL_RECORD, DUPLICATE_PROCESSOR_RECORD, AMOUNT_MISMATCH
}
