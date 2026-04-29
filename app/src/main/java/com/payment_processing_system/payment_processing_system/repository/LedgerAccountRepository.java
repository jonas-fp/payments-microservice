package com.payment_processing_system.payment_processing_system.repository;

import java.util.Optional;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.LedgerAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {
    Optional<LedgerAccountEntity> findByAccountCode(String accountCode);
}
