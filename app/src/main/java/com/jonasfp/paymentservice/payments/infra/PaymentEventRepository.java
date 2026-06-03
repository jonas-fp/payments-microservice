package com.jonasfp.paymentservice.payments.infra;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jonasfp.paymentservice.payments.domain.PaymentEvent;

public interface PaymentEventRepository
        extends JpaRepository<PaymentEvent, UUID> {
}
