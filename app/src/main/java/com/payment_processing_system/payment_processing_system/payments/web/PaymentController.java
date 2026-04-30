package com.payment_processing_system.payment_processing_system.payments.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment_processing_system.payment_processing_system.payments.application.PaymentService;
import com.payment_processing_system.payment_processing_system.payments.web.dto.AuthorizePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CapturePaymentRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.CaptureResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.PaymentResponse;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundRequest;
import com.payment_processing_system.payment_processing_system.payments.web.dto.RefundResponse;

import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody AuthorizePaymentRequest request) {

        try {
            PaymentResponse response = paymentService.authorize(idempotencyKey,
                request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("already in progress")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else if (e.getMessage().contains("different request body")) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
            }
            throw e;
        }
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<CaptureResponse> capture(@PathVariable UUID paymentId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody CapturePaymentRequest request) {

        try {
            CaptureResponse response = paymentService.capture(paymentId,
                idempotencyKey, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("already in progress")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else if (e.getMessage().contains("different request body")) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
            } else if (e.getMessage().contains("AUTHORIZED state")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> refund(@PathVariable UUID paymentId,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody RefundRequest request) {

        try {
            RefundResponse response =
                paymentService.refund(paymentId, idempotencyKey, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("already in progress")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            } else if (e.getMessage().contains("different request body")) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
            } else if (e.getMessage().contains("refundable state")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
