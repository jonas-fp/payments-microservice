package com.payment_processing_system.payment_processing_system.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByProcessorRefundReference(
        String processorRefundReference);

    List<RefundEntity> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
