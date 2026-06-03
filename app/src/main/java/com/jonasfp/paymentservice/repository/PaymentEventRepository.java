package com.jonasfp.paymentservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.entity.PaymentEventEntity;

public interface PaymentEventRepository
        extends JpaRepository<PaymentEventEntity, UUID> {
}
