package com.payment_processing_system.payment_processing_system.reconciliation.infra;

import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ReconciliationBreakRepository
    extends JpaRepository<ReconciliationBreak, UUID> {
    @Query("""
        SELECT b.breakType, COUNT(b) FROM ReconciliationBreak b
        WHERE b.reconciliationRun.id = :runId GROUP BY b.breakType
        """)
    List<Object[]> countBreaksByType(@Param("runId") UUID runId);
}
