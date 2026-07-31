package com.jonasfp.paymentservice.payments.infra;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.payments.domain.IdempotencyActionType;
import com.jonasfp.paymentservice.payments.domain.IdempotencyKey;

public interface IdempotencyKeyRepository
    extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByCustomerIdAndIdempotencyKeyAndActionType(
        String customerId, String idempotencyKey,
        IdempotencyActionType actionType);
}
