package com.payment_processing_system.payment_processing_system.repository;

import java.util.Optional;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByCustomerIdAndIdempotencyKeyAndActionType(
            String customerId, String idempotencyKey, String actionType);
}
