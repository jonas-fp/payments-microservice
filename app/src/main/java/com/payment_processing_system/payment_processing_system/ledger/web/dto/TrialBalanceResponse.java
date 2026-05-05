package com.payment_processing_system.payment_processing_system.ledger.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record TrialBalanceResponse(
    List<TrialBalanceEntry> entries,
    OffsetDateTime asOf
) {}
