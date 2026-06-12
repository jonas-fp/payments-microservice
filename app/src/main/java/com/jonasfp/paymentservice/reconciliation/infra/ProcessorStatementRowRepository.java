package com.jonasfp.paymentservice.reconciliation.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.reconciliation.domain.ProcessorStatementRow;
import java.util.List;
import java.util.UUID;

public interface ProcessorStatementRowRepository
    extends JpaRepository<ProcessorStatementRow, UUID> {
    List<ProcessorStatementRow> findAllByReconciliationRunId(
        UUID reconciliationRunId);
}
