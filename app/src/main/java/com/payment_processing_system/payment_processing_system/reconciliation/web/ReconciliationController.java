package com.payment_processing_system.payment_processing_system.reconciliation.web;

import com.payment_processing_system.payment_processing_system.reconciliation.application.ReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
}
