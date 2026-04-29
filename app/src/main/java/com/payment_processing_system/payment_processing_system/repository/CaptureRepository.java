package com.payment_processing_system.payment_processing_system.repository;

import java.util.UUID;

import com.payment_processing_system.payment_processing_system.entity.CaptureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureRepository extends JpaRepository<CaptureEntity, UUID> {
}
