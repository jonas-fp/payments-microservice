package com.jonasfp.paymentservice.payments.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.payments.domain.Capture;

public interface CaptureRepository extends JpaRepository<Capture, UUID> {
    Optional<Capture> findByProcessorCaptureReference(
        String processorCaptureReference);

    List<Capture> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
