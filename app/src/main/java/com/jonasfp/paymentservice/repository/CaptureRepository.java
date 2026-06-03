package com.jonasfp.paymentservice.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.entity.CaptureEntity;

public interface CaptureRepository extends JpaRepository<CaptureEntity, UUID> {
    Optional<CaptureEntity> findByProcessorCaptureReference(
        String processorCaptureReference);

    List<CaptureEntity> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
