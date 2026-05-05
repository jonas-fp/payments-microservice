package com.payment_processing_system.payment_processing_system.ledger.web.dto;

import java.math.BigDecimal;

public record TrialBalanceEntry(
    String accountCode,
    String accountName,
    BigDecimal totalDebit,
    BigDecimal totalCredit
) {}
