package com.jonasfp.paymentservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.entity.RefundEntity;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByProcessorRefundReference(
        String processorRefundReference);

    List<RefundEntity> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
