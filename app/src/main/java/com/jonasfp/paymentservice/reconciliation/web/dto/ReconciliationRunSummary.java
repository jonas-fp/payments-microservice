package com.jonasfp.paymentservice.reconciliation.web.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationBreakType;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRunStatus;

public record ReconciliationRunSummary(
    UUID id,
    LocalDate businessDate,
    ReconciliationRunStatus status,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    Map<ReconciliationBreakType, Long> breakSummary
) {}
