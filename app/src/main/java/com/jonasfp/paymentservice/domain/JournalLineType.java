package com.jonasfp.paymentservice.domain;

public enum JournalLineType {
    DEBIT, CREDIT;

    public JournalLineType opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
