package com.payment_processing_system.payment_processing_system.domain;

public enum JournalLineType {
    DEBIT, CREDIT;

    public JournalLineType opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
