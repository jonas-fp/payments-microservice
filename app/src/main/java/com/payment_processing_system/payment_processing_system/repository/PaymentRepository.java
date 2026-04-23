package com.payment_processing_system.payment_processing_system.repository;

import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository
        extends JpaRepository<PaymentEntity, UUID> {
}
