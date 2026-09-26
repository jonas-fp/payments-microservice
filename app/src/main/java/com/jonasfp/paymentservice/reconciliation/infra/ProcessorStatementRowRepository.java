package com.jonasfp.paymentservice.reconciliation.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.reconciliation.domain.ProcessorStatementRow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ProcessorStatementRowRepository
    extends JpaRepository<ProcessorStatementRow, UUID> {

    List<ProcessorStatementRow> findAllByReconciliationRunId(
        UUID reconciliationRunId);

    default Map<String, ProcessorStatementRow> findAllByReconciliationRunIdAsMap(
        UUID reconciliationRunId) {
        return findAllByReconciliationRunId(reconciliationRunId).stream()
            .collect(Collectors.toMap(
                ProcessorStatementRow::getProcessorReference,
                Function.identity()));
    }
}
