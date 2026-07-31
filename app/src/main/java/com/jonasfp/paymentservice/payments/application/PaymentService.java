package com.jonasfp.paymentservice.payments.application;

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
import com.jonasfp.paymentservice.domain.JournalLineType;
import com.jonasfp.paymentservice.domain.Money;
import com.jonasfp.paymentservice.domain.PaymentEventType;
import com.jonasfp.paymentservice.domain.PaymentStatus;
import com.jonasfp.paymentservice.domain.TransactionType;
import com.jonasfp.paymentservice.ledger.domain.JournalEntry;
import com.jonasfp.paymentservice.ledger.domain.JournalLine;
import com.jonasfp.paymentservice.ledger.domain.LedgerAccount;
import com.jonasfp.paymentservice.ledger.domain.LedgerAccountCodes;
import com.jonasfp.paymentservice.payments.domain.Capture;
import com.jonasfp.paymentservice.payments.domain.IdempotencyActionType;
import com.jonasfp.paymentservice.payments.domain.IdempotencyKey;
import com.jonasfp.paymentservice.payments.domain.IdempotencyResponseStatus;
import com.jonasfp.paymentservice.payments.domain.Payment;
import com.jonasfp.paymentservice.payments.domain.PaymentEvent;
import com.jonasfp.paymentservice.payments.domain.Refund;
import com.jonasfp.paymentservice.payments.web.dto.AuthorizePaymentRequest;
import com.jonasfp.paymentservice.payments.web.dto.CapturePaymentRequest;
import com.jonasfp.paymentservice.payments.web.dto.CaptureResponse;
import com.jonasfp.paymentservice.payments.web.dto.PaymentResponse;
import com.jonasfp.paymentservice.payments.web.dto.RefundRequest;
import com.jonasfp.paymentservice.payments.web.dto.RefundResponse;
import com.jonasfp.paymentservice.payments.infra.CaptureRepository;
import com.jonasfp.paymentservice.payments.infra.IdempotencyKeyRepository;
import com.jonasfp.paymentservice.ledger.infra.JournalEntryRepository;
import com.jonasfp.paymentservice.ledger.infra.JournalLineRepository;
import com.jonasfp.paymentservice.ledger.infra.LedgerAccountRepository;
import com.jonasfp.paymentservice.payments.infra.PaymentEventRepository;
import com.jonasfp.paymentservice.payments.infra.PaymentRepository;
import com.jonasfp.paymentservice.payments.infra.RefundRepository;

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
        IdempotencyActionType actionType = IdempotencyActionType.AUTHORIZE;
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();

            // Validate request hash to ensure the same key isn't used for a
            // different request
            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                    "Idempotency key reuse with different request body");
            }

            if (IdempotencyResponseStatus.COMPLETED
                .equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                        PaymentResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(
                        "Failed to deserialize cached response", e);
                }
            } else if (IdempotencyResponseStatus.STARTED
                .equals(key.getResponseStatus())) {
                throw new IllegalStateException("Request already in progress");
            }
        }

        // 2. Register idempotency key as STARTED
        IdempotencyKey keyEntity = new IdempotencyKey();
        keyEntity.setCustomerId(request.customerId());
        keyEntity.setIdempotencyKey(idempotencyKey);
        keyEntity.setActionType(actionType);
        keyEntity.setRequestHash(requestHash);
        keyEntity.setResponseStatus(IdempotencyResponseStatus.STARTED);
        keyEntity = idempotencyKeyRepository.save(keyEntity);

        // 3. Process the authorization (Mocking processor call)
        String processorReference = "proc_" + idempotencyKey.substring(0, 8);

        // 4. Create Payment
        Money moneyToAuthorize =
            Money.fromMinor(request.minorAmount(), request.currency());
        Payment payment = new Payment();
        payment.setCustomerId(request.customerId());
        payment.setInvoiceId(request.invoiceId());
        payment.setAuthorizedAmount(moneyToAuthorize.majorAmount());
        payment.setCapturedAmount(BigDecimal.ZERO);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setProcessorPaymentReference(processorReference);
        payment = paymentRepository.save(payment);

        // 5. Create Payment Event
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.AUTHORIZE_SUCCESS);
        event.setProcessorEventReference(processorReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        paymentEventRepository.save(event);

        // 6. Complete Idempotency Key
        PaymentResponse response = new PaymentResponse(payment.getId(),
            payment.getCustomerId(), payment.getInvoiceId(),
            moneyToAuthorize.toMinor(),
            payment.getCurrency(), payment.getStatus(),
            payment.getProcessorPaymentReference());

        keyEntity.setResponseStatus(IdempotencyResponseStatus.COMPLETED);
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
        IdempotencyActionType actionType = IdempotencyActionType.CAPTURE;
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();

            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                    "Idempotency key reuse with different request body");
            }

            if (IdempotencyResponseStatus.COMPLETED
                .equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                        CaptureResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(
                        "Failed to deserialize cached response", e);
                }
            } else if (IdempotencyResponseStatus.STARTED
                .equals(key.getResponseStatus())) {
                throw new IllegalStateException("Request already in progress");
            }
        }

        // 2. Register idempotency key as STARTED
        IdempotencyKey keyEntity = new IdempotencyKey();
        keyEntity.setCustomerId(request.customerId());
        keyEntity.setIdempotencyKey(idempotencyKey);
        keyEntity.setActionType(actionType);
        keyEntity.setRequestHash(requestHash);
        keyEntity.setResponseStatus(IdempotencyResponseStatus.STARTED);
        keyEntity = idempotencyKeyRepository.save(keyEntity);

        // 3. Validation
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Payment not found"));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException(
                "Payment is not in AUTHORIZED state");
        }

        Money moneyToCapture =
            Money.fromMinor(request.minorAmount(), request.currency());
        if (moneyToCapture.majorAmount()
            .compareTo(payment.getAuthorizedAmount()) != 0) {
            throw new IllegalArgumentException(
                "Capture amount must equal authorized amount");
        }

        // 4. Process the capture (Mocking processor call)
        String processorCaptureReference =
            "cap_" + idempotencyKey.substring(0, 8);

        // 5. Create Payment Event
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.CAPTURE_SUCCESS);
        event.setProcessorEventReference(processorCaptureReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        event = paymentEventRepository.save(event);

        // 6. Record Capture
        Capture capture = new Capture();
        capture.setPaymentId(payment.getId());
        capture.setPaymentEventId(event.getId());
        capture.setAmount(moneyToCapture.majorAmount());
        capture.setCurrency(payment.getCurrency());
        capture.setProcessorCaptureReference(processorCaptureReference);
        capture = captureRepository.save(capture);

        // 7. Ledger Entry (Double-Entry)
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setPaymentId(payment.getId());
        journalEntry.setCaptureId(capture.getId());
        journalEntry.setTransactionType(TransactionType.CAPTURE);
        journalEntry = journalEntryRepository.save(journalEntry);

        LedgerAccount cashClearing = ledgerAccountRepository
            .findByAccountCode(LedgerAccountCodes.CASH_CLEARING)
            .orElseThrow(() -> new IllegalStateException(
                "Cash Clearing account not found"));
        LedgerAccount deferredRevenue = ledgerAccountRepository
            .findByAccountCode(LedgerAccountCodes.DEFERRED_REVENUE)
            .orElseThrow(() -> new IllegalStateException(
                "Deferred Revenue account not found"));

        // DEBIT Cash Clearing
        JournalLine debitLine = new JournalLine();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setLedgerAccountId(cashClearing.getId());
        debitLine.setDirection(JournalLineType.DEBIT);
        debitLine.setAmount(moneyToCapture.majorAmount());
        debitLine.setCurrency(moneyToCapture.currency().value());
        journalLineRepository.save(debitLine);

        // CREDIT Deferred Revenue
        JournalLine creditLine = new JournalLine();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setLedgerAccountId(deferredRevenue.getId());
        creditLine.setDirection(JournalLineType.CREDIT);
        creditLine.setAmount(moneyToCapture.majorAmount());
        creditLine.setCurrency(moneyToCapture.currency().value());
        journalLineRepository.save(creditLine);

        // 8. Complete Idempotency Key
        CaptureResponse response = new CaptureResponse(capture.getId(),
            payment.getId(), moneyToCapture.toMinor(),
            capture.getCurrency(), PaymentStatus.CAPTURED, // Status will be
                                                           // updated by DB
                                                           // trigger
            capture.getProcessorCaptureReference());

        keyEntity.setResponseStatus(IdempotencyResponseStatus.COMPLETED);
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
        IdempotencyActionType actionType = IdempotencyActionType.REFUND;
        String requestHash = calculateHash(request);

        // 1. Check for existing idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
            .findByCustomerIdAndIdempotencyKeyAndActionType(
                request.customerId(), idempotencyKey, actionType);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();

            if (!key.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException(
                    "Idempotency key reuse with different request body");
            }

            if (IdempotencyResponseStatus.COMPLETED
                .equals(key.getResponseStatus())) {
                try {
                    return objectMapper.treeToValue(key.getResponseBody(),
                        RefundResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(
                        "Failed to deserialize cached response", e);
                }
            } else if (IdempotencyResponseStatus.STARTED
                .equals(key.getResponseStatus())) {
                throw new IllegalStateException("Request already in progress");
            }
        }

        // 2. Register idempotency key as STARTED
        IdempotencyKey keyEntity = new IdempotencyKey();
        keyEntity.setCustomerId(request.customerId());
        keyEntity.setIdempotencyKey(idempotencyKey);
        keyEntity.setActionType(actionType);
        keyEntity.setRequestHash(requestHash);
        keyEntity.setResponseStatus(IdempotencyResponseStatus.STARTED);
        keyEntity = idempotencyKeyRepository.save(keyEntity);

        // 3. Validation
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found"));

        if (!payment.getStatus().canBeRefunded()) {
            throw new IllegalStateException(
                "Payment is not in a refundable state");
        }

        Money moneyToRefund =
            Money.fromMinor(request.minorAmount(), request.currency());
        BigDecimal availableToRefund =
            payment.getCapturedAmount().subtract(payment.getRefundedAmount());
        if (moneyToRefund.majorAmount().compareTo(availableToRefund) > 0) {
            throw new IllegalArgumentException(
                "Refund amount exceeds available captured amount");
        }

        // 4. Process the refund (Mocking processor call)
        String processorRefundReference =
            "ref_" + idempotencyKey.substring(0, 8);

        // 5. Create Payment Event
        PaymentEvent event = new PaymentEvent();
        event.setPaymentId(payment.getId());
        event.setEventType(PaymentEventType.REFUND_SUCCESS);
        event.setProcessorEventReference(processorRefundReference + "_evt");
        event.setIdempotencyKeyId(keyEntity.getId());
        event = paymentEventRepository.save(event);

        // 6. Record Refund
        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setPaymentEventId(event.getId());
        refund.setAmount(moneyToRefund.majorAmount());
        refund.setCurrency(payment.getCurrency());
        refund.setProcessorRefundReference(processorRefundReference);
        refund = refundRepository.save(refund);

        // 7. Ledger Entry (Double-Entry)
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setPaymentId(payment.getId());
        journalEntry.setRefundId(refund.getId());
        journalEntry.setTransactionType(TransactionType.REFUND);
        journalEntry = journalEntryRepository.save(journalEntry);

        LedgerAccount cashClearing =
            ledgerAccountRepository
                .findByAccountCode(LedgerAccountCodes.CASH_CLEARING)
                .orElseThrow(() -> new IllegalStateException(
                    "Cash Clearing account not found"));
        LedgerAccount deferredRevenue =
            ledgerAccountRepository
                .findByAccountCode(LedgerAccountCodes.DEFERRED_REVENUE)
                .orElseThrow(() -> new IllegalStateException(
                    "Deferred Revenue account not found"));

        Money money =
            Money.of(moneyToRefund.majorAmount(), payment.getCurrency());

        // DEBIT Deferred Revenue (Liability decreases)
        JournalLine debitLine = new JournalLine();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setLedgerAccountId(deferredRevenue.getId());
        debitLine.setDirection(JournalLineType.DEBIT);
        debitLine.setAmount(money.majorAmount());
        debitLine.setCurrency(money.currency().value());
        journalLineRepository.save(debitLine);

        // CREDIT Cash Clearing (Asset decreases)
        JournalLine creditLine = new JournalLine();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setLedgerAccountId(cashClearing.getId());
        creditLine.setDirection(JournalLineType.CREDIT);
        creditLine.setAmount(money.majorAmount());
        creditLine.setCurrency(money.currency().value());
        journalLineRepository.save(creditLine);

        // 8. Complete Idempotency Key
        RefundResponse response = new RefundResponse(
            refund.getId(),
            payment.getId(),
            moneyToRefund.toMinor(),
            refund.getCurrency(),
            payment.getCapturedAmount()
                .subtract(payment.getRefundedAmount()
                    .add(moneyToRefund.majorAmount()))
                .compareTo(BigDecimal.ZERO) == 0
                    ? PaymentStatus.FULLY_REFUNDED
                    : PaymentStatus.PARTIALLY_REFUNDED,
            refund.getProcessorRefundReference());

        keyEntity.setResponseStatus(IdempotencyResponseStatus.COMPLETED);
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
