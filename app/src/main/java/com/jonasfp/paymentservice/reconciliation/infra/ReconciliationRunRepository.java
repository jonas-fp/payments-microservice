package com.jonasfp.paymentservice.reconciliation.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRun;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRunStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationRunRepository
    extends JpaRepository<ReconciliationRun, UUID> {
    Optional<ReconciliationRun> findByBusinessDateAndStatus(
        LocalDate businessDate, ReconciliationRunStatus status);
}
