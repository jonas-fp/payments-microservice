package com.jonasfp.paymentservice.reconciliation.infra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationBreak;
import java.util.UUID;
import java.util.List;

public interface ReconciliationBreakRepository
    extends JpaRepository<ReconciliationBreak, UUID> {
    @Query("""
        SELECT b.breakType, COUNT(b) FROM ReconciliationBreak b
        WHERE b.reconciliationRun.id = :runId GROUP BY b.breakType
        """)
    List<Object[]> countBreaksByType(@Param("runId") UUID runId);
}
