package com.jonasfp.paymentservice.ledger.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.jonasfp.paymentservice.ledger.domain.JournalLine;

public interface JournalLineRepository
    extends JpaRepository<JournalLine, UUID> {
    @Query("""
        SELECT jl FROM JournalLine jl
        WHERE jl.ledgerAccountId = :accountId
        AND jl.createdAt <= :asOf
        """)
    List<JournalLine> findByLedgerAccountIdAndCreatedAtBefore(
        @Param("accountId") UUID accountId,
        @Param("asOf") OffsetDateTime asOf);

    @Query("""
        SELECT new com.jonasfp.paymentservice.ledger.web.dto.TrialBalanceEntry(
            la.accountCode,
            la.accountName,
            SUM(CASE WHEN jl.direction = 'DEBIT' THEN jl.amount ELSE 0 END),
            SUM(CASE WHEN jl.direction = 'CREDIT' THEN jl.amount ELSE 0 END)
        )
        FROM LedgerAccount la
        LEFT JOIN JournalLine jl ON la.id = jl.ledgerAccountId AND jl.createdAt <= :asOf
        GROUP BY la.accountCode, la.accountName
        """)
    List<com.jonasfp.paymentservice.ledger.web.dto.TrialBalanceEntry> getTrialBalance(
        @Param("asOf") OffsetDateTime asOf);
}
