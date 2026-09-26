package com.jonasfp.paymentservice.reconciliation.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRun;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRunStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRunRepository
    extends JpaRepository<ReconciliationRun, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReconciliationRun> findWithLockFirstByBusinessDateAndStatus(
        LocalDate businessDate, ReconciliationRunStatus status);

    Optional<ReconciliationRun> findFirstByBusinessDateAndStatus(
        LocalDate businessDate, ReconciliationRunStatus status);
}
