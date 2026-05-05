package com.payment_processing_system.payment_processing_system.ledger.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.payment_processing_system.payment_processing_system.ledger.application.LedgerService;
import com.payment_processing_system.payment_processing_system.ledger.web.dto.AccountBalanceResponse;

@RestController
@RequestMapping("/v1/payments/subledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
        @PathVariable UUID accountId,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime asOf) {

        if (asOf == null) {
            asOf = OffsetDateTime.now();
        }

        try {
            AccountBalanceResponse response =
                ledgerService.getAccountBalance(accountId, asOf);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
