package com.payment_processing_system.payment_processing_system.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.CaptureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureRepository extends JpaRepository<CaptureEntity, UUID> {
    Optional<CaptureEntity> findByProcessorCaptureReference(
        String processorCaptureReference);

    List<CaptureEntity> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
