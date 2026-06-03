package com.jonasfp.paymentservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.entity.IdempotencyKeyEntity;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity> findByCustomerIdAndIdempotencyKeyAndActionType(
            String customerId, String idempotencyKey, String actionType);
}
