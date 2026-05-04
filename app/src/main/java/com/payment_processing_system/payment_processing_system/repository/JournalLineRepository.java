package com.payment_processing_system.payment_processing_system.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalLineRepository
    extends JpaRepository<JournalLineEntity, UUID> {
    @Query(
        """
        SELECT jl FROM JournalLineEntity jl 
        WHERE jl.ledgerAccountId = :accountId 
        AND jl.createdAt <= :asOf
        """
    )
    List<JournalLineEntity> findByLedgerAccountIdAndCreatedAtBefore(
        @Param("accountId") UUID accountId,
        @Param("asOf") OffsetDateTime asOf);
}
