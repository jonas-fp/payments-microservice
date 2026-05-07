package com.payment_processing_system.payment_processing_system.reconciliation.web;

import com.payment_processing_system.payment_processing_system.reconciliation.application.ReconciliationService;
import com.payment_processing_system.payment_processing_system.reconciliation.web.dto.ReconciliationRunSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
        ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping(value = "/imports",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, UUID>> importStatement(
        @RequestParam("businessDate") @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE) LocalDate businessDate,
        @RequestParam("file") MultipartFile file) {

        try {
            UUID runId =
                reconciliationService.importStatement(businessDate, file);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("runId", runId));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/runs")
    public ResponseEntity<Map<String, UUID>> runReconciliation(
        @RequestParam @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {
        try {
            UUID runId = reconciliationService.runReconciliation(businessDate);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("runId", runId));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("No PENDING reconciliation")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            throw e;
        }
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<ReconciliationRunSummary> getRun(
        @PathVariable UUID runId) {
        try {
            ReconciliationRunSummary summary =
                reconciliationService.getRunSummary(runId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
