package com.payment_processing_system.payment_processing_system.reconciliation.web.dto;

import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRunStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ReconciliationRunSummary(
    UUID id,
    LocalDate businessDate,
    ReconciliationRunStatus status,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    Map<String, Long> breakSummary
) {}
