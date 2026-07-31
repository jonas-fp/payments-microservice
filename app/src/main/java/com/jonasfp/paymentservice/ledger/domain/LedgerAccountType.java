package com.jonasfp.paymentservice.ledger.domain;

public enum LedgerAccountType {
    ASSET(false), LIABILITY(true), EQUITY(true), REVENUE(true), EXPENSE(false);

    private final boolean creditNormal;

    LedgerAccountType(boolean creditNormal) {
        this.creditNormal = creditNormal;
    }

    public boolean isCreditNormal() {
        return creditNormal;
    }
}
