package com.payment_processing_system.payment_processing_system.reconciliation.infra;

import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRun;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRunRepository
    extends JpaRepository<ReconciliationRun, UUID> {
    Optional<ReconciliationRun> findByBusinessDateAndStatus(
        LocalDate businessDate, ReconciliationRunStatus status);
}
