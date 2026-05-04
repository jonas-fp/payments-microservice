package com.payment_processing_system.payment_processing_system.ledger.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountBalanceResponse(
    UUID accountId,
    String accountCode,
    BigDecimal balance,
    String currency,
    OffsetDateTime asOf
) {}
