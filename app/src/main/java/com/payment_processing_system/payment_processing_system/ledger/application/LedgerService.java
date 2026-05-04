package com.payment_processing_system.payment_processing_system.ledger.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment_processing_system.payment_processing_system.domain.JournalLineType;
import com.payment_processing_system.payment_processing_system.entity.JournalLineEntity;
import com.payment_processing_system.payment_processing_system.entity.LedgerAccountEntity;
import com.payment_processing_system.payment_processing_system.ledger.web.dto.AccountBalanceResponse;
import com.payment_processing_system.payment_processing_system.repository.JournalLineRepository;
import com.payment_processing_system.payment_processing_system.repository.LedgerAccountRepository;

@Service
public class LedgerService {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalLineRepository journalLineRepository;

    public LedgerService(LedgerAccountRepository ledgerAccountRepository,
        JournalLineRepository journalLineRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalLineRepository = journalLineRepository;
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(UUID accountId,
        OffsetDateTime asOf) {
        LedgerAccountEntity account =
            ledgerAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Account not found: " + accountId));

        List<JournalLineEntity> lines = journalLineRepository
            .findByLedgerAccountIdAndCreatedAtBefore(accountId, asOf);

        BigDecimal balance = BigDecimal.ZERO;
        String currency = "USD";

        for (JournalLineEntity line : lines) {
            currency = line.getCurrency();
            if (line.getDirection() == JournalLineType.DEBIT) {
                balance = balance.add(line.getAmount());
            } else {
                balance = balance.subtract(line.getAmount());
            }
        }

        if ("LIABILITY".equals(account.getAccountType()) ||
            "EQUITY".equals(account.getAccountType()) ||
            "REVENUE".equals(account.getAccountType())) {
            balance = balance.negate();
        }

        return new AccountBalanceResponse(
            account.getId(),
            account.getAccountCode(),
            balance,
            currency,
            asOf);
    }
}
