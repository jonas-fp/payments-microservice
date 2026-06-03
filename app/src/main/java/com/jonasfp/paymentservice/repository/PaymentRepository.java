package com.jonasfp.paymentservice.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.entity.PaymentEntity;

public interface PaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {
}
