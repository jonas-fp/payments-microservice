package com.payment_processing_system.payment_processing_system.reconciliation.infra;

import com.payment_processing_system.payment_processing_system.reconciliation.domain.ProcessorStatementRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessorStatementRowRepository
    extends JpaRepository<ProcessorStatementRow, UUID> {
    List<ProcessorStatementRow> findAllByReconciliationRunId(
        UUID reconciliationRunId);
}
