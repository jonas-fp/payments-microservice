package com.jonasfp.paymentservice.payments.infra;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.payments.domain.Refund;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByProcessorRefundReference(
        String processorRefundReference);

    List<Refund> findAllByCreatedAtBetween(OffsetDateTime start,
        OffsetDateTime end);
}
