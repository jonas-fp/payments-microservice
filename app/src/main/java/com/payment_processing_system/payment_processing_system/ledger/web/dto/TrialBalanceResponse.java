package com.payment_processing_system.payment_processing_system.ledger.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TrialBalanceResponse(
    OffsetDateTime asOf,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    Boolean isBalanced,
    List<TrialBalanceEntry> entries
) {}
