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
import com.payment_processing_system.payment_processing_system.domain.CurrencyCode;
import com.payment_processing_system.payment_processing_system.domain.JournalLineType;
import com.payment_processing_system.payment_processing_system.domain.Money;
import com.payment_processing_system.payment_processing_system.domain.PaymentEventType;
import com.payment_processing_system.payment_processing_system.domain.PaymentStatus;
import com.payment_processing_system.payment_processing_system.domain.TransactionType;
import com.payment_processing_system.payment_processing_system.entity.CaptureEntity;
import com.payment_processing_system.payment_processing_system.entity.IdempotencyKeyEntity;
import com.payment_processing_system.payment_processing_system.entity.JournalEntryEntity;
import com.payment_processing_system.payment_processing_system.entity.JournalLineEntity;
import com.payment_processing_system.payment_processing_system.entity.LedgerAccountEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEntity;
import com.payment_processing_system.payment_processing_system.entity.PaymentEventEntity;
import com.payment_processing_system.payment_processing_system.entity.RefundEntity;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CapturePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CaptureResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundResponse;
import com.payment_processing_system.payment_processing_system.repository.CaptureRepository;
import com.payment_processing_system.payment_processing_system.repository.IdempotencyKeyRepository;
import com.payment_processing_system.payment_processing_system.repository.JournalEntryRepository;
import com.payment_processing_system.payment_processing_system.repository.JournalLineRepository;
import com.payment_processing_system.payment_processing_system.repository.LedgerAccountRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentEventRepository;
import com.payment_processing_system.payment_processing_system.repository.PaymentRepository;
import com.payment_processing_system.payment_processing_system.repository.RefundRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final CaptureRepository captureRepository;
    private final RefundRepository refundRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
        PaymentEventRepository paymentEventRepository,
        IdempotencyKeyRepository idempotencyKeyRepository,
        CaptureRepository captureRepository,
        RefundRepository refundRepository,
        JournalEntryRepository journalEntryRepository,
        JournalLineRepository journalLineRepository,
        LedgerAccountRepository ledgerAccountRepository,
        ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.captureRepository = captureRepository;
        this.refundRepository = refundRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
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
        String processorReference = "proc_" + idempotencyKey.substring(0, 8);

        // 4. Create Payment
        PaymentEntity payment = new PaymentEntity();
        payment.setCustomerId(request.customerId());
        payment.setInvoiceId(request.invoiceId());
        payment.setAuthorizedAmount(request.amountMinor().movePointLeft(2));
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
            payment.getAuthorizedAmount().movePointRight(2),
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

    @Transactional
    public CaptureResponse capture(UUID paymentId, String idempotencyKey,
        CapturePaymentRequest request) {
        String actionType = "CAPTURE";
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKeyEntity> existingKey = idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKeyEntity key = existingKey.get();

            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                    "Idempotency key reuse with different request body");
            }

            if ("COMPLETED".equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                        CaptureResponse.class);
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

        // 3. Validation
        PaymentEntity payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment not found"));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Payment is not in AUTHORIZED state");
        }

        BigDecimal amountToCapture = request.amountMinor().movePointLeft(2);
        if (amountToCapture.compareTo(payment.getAuthorizedAmount()) != 0) {
            throw new IllegalArgumentException(
                "Only full captures are currently supported");
        }

        // 4. Process the capture (Mocking processor call)
        String processorCaptureReference =
            "cap_" + idempotencyKey.substring(0, 8);

        // 5. Create Payment Event
        PaymentEventEntity event = new PaymentEventEntity();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.CAPTURE_SUCCESS);
        event.setProcessorEventReference(processorCaptureReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        event = paymentEventRepository.save(event);

        // 6. Record Capture
        CaptureEntity capture = new CaptureEntity();
        capture.setPaymentId(payment.getId());
        capture.setPaymentEventId(event.getId());
        capture.setAmount(amountToCapture);
        capture.setCurrency(payment.getCurrency());
        capture.setProcessorCaptureReference(processorCaptureReference);
        capture = captureRepository.save(capture);

        // 7. Ledger Entry (Double-Entry)
        JournalEntryEntity journalEntry = new JournalEntryEntity();
        journalEntry.setPaymentId(payment.getId());
        journalEntry.setCaptureId(capture.getId());
        journalEntry.setTransactionType(TransactionType.CAPTURE);
        journalEntry = journalEntryRepository.save(journalEntry);

        LedgerAccountEntity cashClearing = ledgerAccountRepository
            .findByAccountCode("10001")
            .orElseThrow(() -> new IllegalStateException(
                "Cash Clearing account not found"));
        LedgerAccountEntity deferredRevenue = ledgerAccountRepository
            .findByAccountCode("20002")
            .orElseThrow(() -> new IllegalStateException(
                "Deferred Revenue account not found"));

        Money money = new Money(amountToCapture,
            new CurrencyCode(payment.getCurrency()));

        // DEBIT Cash Clearing
        JournalLineEntity debitLine = new JournalLineEntity();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setLedgerAccountId(cashClearing.getId());
        debitLine.setDirection(JournalLineType.DEBIT);
        debitLine.setAmount(money.amountMinor());
        debitLine.setCurrency(money.currency().value());
        journalLineRepository.save(debitLine);

        // CREDIT Deferred Revenue
        JournalLineEntity creditLine = new JournalLineEntity();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setLedgerAccountId(deferredRevenue.getId());
        creditLine.setDirection(JournalLineType.CREDIT);
        creditLine.setAmount(money.amountMinor());
        creditLine.setCurrency(money.currency().value());
        journalLineRepository.save(creditLine);

        // 8. Complete Idempotency Key
        CaptureResponse response = new CaptureResponse(capture.getId(),
            payment.getId(), capture.getAmount().movePointRight(2),
            capture.getCurrency(), PaymentStatus.CAPTURED, // Status will be
                                                           // updated by DB
                                                           // trigger
            capture.getProcessorCaptureReference());

        keyEntity.setResponseStatus("COMPLETED");
        keyEntity.setResourceId(capture.getId());
        keyEntity.setEventId(event.getId());
        keyEntity.setResponseCode(201);
        keyEntity.setResponseBody(objectMapper.valueToTree(response));
        idempotencyKeyRepository.save(keyEntity);

        return response;
    }

    @Transactional
    public RefundResponse refund(UUID paymentId, String idempotencyKey,
        RefundRequest request) {
        String actionType = "REFUND";
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKeyEntity> existingKey = idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKeyEntity key = existingKey.get();

            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                    "Idempotency key reuse with different request body");
            }

            if ("COMPLETED".equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                        RefundResponse.class);
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

        // 3. Validation
        PaymentEntity payment = paymentRepository.findById(paymentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found"));

        if (!payment.getStatus().canBeRefunded()) {
            throw new IllegalStateException(
                "Payment is not in a refundable state");
        }

        BigDecimal amountToRefund = request.amountMinor().movePointLeft(2);
        BigDecimal availableToRefund =
            payment.getCapturedAmount().subtract(payment.getRefundedAmount());
        if (amountToRefund.compareTo(availableToRefund) > 0) {
            throw new IllegalArgumentException(
                "Refund amount exceeds available captured amount");
        }

        // 4. Process the refund (Mocking processor call)
        String processorRefundReference =
            "ref_" + idempotencyKey.substring(0, 8);

        // 5. Create Payment Event
        PaymentEventEntity event = new PaymentEventEntity();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.REFUND_SUCCESS);
        event.setProcessorEventReference(processorRefundReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        event = paymentEventRepository.save(event);

        // 6. Record Refund
        RefundEntity refund = new RefundEntity();
        refund.setPaymentId(payment.getId());
        refund.setPaymentEventId(event.getId());
        refund.setAmount(amountToRefund);
        refund.setCurrency(payment.getCurrency());
        refund.setProcessorRefundReference(processorRefundReference);
        refund = refundRepository.save(refund);

        // 7. Ledger Entry (Double-Entry)
        JournalEntryEntity journalEntry = new JournalEntryEntity();
        journalEntry.setPaymentId(payment.getId());
        journalEntry.setRefundId(refund.getId());
        journalEntry.setTransactionType(TransactionType.REFUND);
        journalEntry = journalEntryRepository.save(journalEntry);

        LedgerAccountEntity cashClearing =
            ledgerAccountRepository.findByAccountCode("10001")
                .orElseThrow(() -> new IllegalStateException(
                    "Cash Clearing account not found"));
        LedgerAccountEntity deferredRevenue =
            ledgerAccountRepository.findByAccountCode("20002")
                .orElseThrow(() -> new IllegalStateException(
                    "Deferred Revenue account not found"));

        Money money =
            new Money(amountToRefund, new CurrencyCode(payment.getCurrency()));

        // DEBIT Deferred Revenue (Liability decreases)
        JournalLineEntity debitLine = new JournalLineEntity();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setLedgerAccountId(deferredRevenue.getId());
        debitLine.setDirection(JournalLineType.DEBIT);
        debitLine.setAmount(money.amountMinor());
        debitLine.setCurrency(money.currency().value());
        journalLineRepository.save(debitLine);

        // CREDIT Cash Clearing (Asset decreases)
        JournalLineEntity creditLine = new JournalLineEntity();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setLedgerAccountId(cashClearing.getId());
        creditLine.setDirection(JournalLineType.CREDIT);
        creditLine.setAmount(money.amountMinor());
        creditLine.setCurrency(money.currency().value());
        journalLineRepository.save(creditLine);

        // 8. Complete Idempotency Key
        RefundResponse response = new RefundResponse(
            refund.getId(),
            payment.getId(),
            refund.getAmount().movePointRight(2),
            refund.getCurrency(),
            payment.getCapturedAmount()
                .subtract(payment.getRefundedAmount().add(amountToRefund))
                .compareTo(BigDecimal.ZERO) == 0
                    ? PaymentStatus.FULLY_REFUNDED
                    : PaymentStatus.PARTIALLY_REFUNDED,
            refund.getProcessorRefundReference());

        keyEntity.setResponseStatus("COMPLETED");
        keyEntity.setResourceId(refund.getId());
        keyEntity.setEventId(event.getId());
        keyEntity.setResponseCode(201);
        keyEntity.setResponseBody(objectMapper.valueToTree(response));
        idempotencyKeyRepository.save(keyEntity);

        return response;
    }

    private String calculateHash(Object request) {
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
