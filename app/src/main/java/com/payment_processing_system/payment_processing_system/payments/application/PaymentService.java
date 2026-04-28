package com.payment_processing_system.payment_processing_system.payments.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_processing_system.payment_processing_system.domain.PaymentEventType;
import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;
import com.payment_processing_system.payment_processing_system.entity.IdempotencyKeyEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEventEntity;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.repository.IdempotencyKeyRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentEventRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentEventRepository paymentEventRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentResponse authorize(String idempotencyKey,
            AuthorizePaymentRequest request) {
        String actionType = "AUTHORIZE";
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKeyEntity> existingKey = idempotencyKeyRepository
                .findByCustomerIdAndIdempotencyKeyAndActionType(
                        request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKeyEntity key = existingKey.get();

            // Validate request hash to ensure the same key isn't used for a
            // different request
            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                        "Idempotency key reuse with different request body");
            }

            if ("COMPLETED".equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                            PaymentResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(
                            "Failed to deserialize cached response", e);
                }
            } else if ("STARTED".equals(key.getResponseStatus())) {
                throw new IllegalStateException("Request already in progress");
            }
        }

        // 2. Register idempotency key as STARTED
        IdempotencyKeyEntity keyEntity = new IdempotencyKeyEntity();
        keyEntity.setCustomerId(request.customerId());
        keyEntity.setIdempotencyKey(idempotencyKey);
        keyEntity.setActionType(actionType);
        keyEntity.setRequestHash(requestHash);
        keyEntity.setResponseStatus("STARTED");
        keyEntity = idempotencyKeyRepository.save(keyEntity);

        // 3. Process the authorization (Mocking processor call)
        String processorReference = "proc_"
                + UUID.randomUUID().toString().substring(0, 8);

        // 4. Create Payment
        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId(request.customerId());
        payment.setInvoiceId(request.invoiceId());
        payment.setAuthorizedAmount(
                request.amountMinor().divide(BigDecimal.valueOf(100)));
        payment.setCapturedAmount(BigDecimal.ZERO);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProcessorPaymentReference(processorReference);
        payment = paymentRepository.save(payment);

        // 5. Create Payment Event
        PaymentEventEntity event = new PaymentEventEntity();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.AUTHORIZE_SUCCESS);
        event.setProcessorEventReference(processorReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        paymentEventRepository.save(event);

        // 6. Complete Idempotency Key
        PaymentResponse response = new PaymentResponse(payment.getId(),
                payment.getCustomerId(), payment.getInvoiceId(),
                payment.getAuthorizedAmount().multiply(BigDecimal.valueOf(100))
                        .stripTrailingZeros(),
                payment.getCurrency(), payment.getStatus(),
                payment.getProcessorPaymentReference());

        keyEntity.setResponseStatus("COMPLETED");
        keyEntity.setResourceId(payment.getId());
        keyEntity.setEventId(event.getId());
        keyEntity.setResponseCode(201);
        keyEntity.setResponseBody(objectMapper.valueToTree(response));
        idempotencyKeyRepository.save(keyEntity);

        return response;
    }

    private String calculateHash(AuthorizePaymentRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String json = objectMapper.writeValueAsString(request);
            byte[] hashBytes = digest
                    .digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new RuntimeException("Failed to calculate request hash", e);
        }
    }
}
