package com.jonasfp.paymentservice.ledger.web.dto;

import java.math.BigDecimal;

public record TrialBalanceEntry(
    String accountCode,
    String accountName,
    BigDecimal totalDebit,
    BigDecimal totalCredit
) {}
