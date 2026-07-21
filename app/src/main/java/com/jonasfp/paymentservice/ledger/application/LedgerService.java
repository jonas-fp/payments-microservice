package com.jonasfp.paymentservice.ledger.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jonasfp.paymentservice.domain.JournalLineType;
import com.jonasfp.paymentservice.domain.Money;
import com.jonasfp.paymentservice.ledger.domain.JournalLine;
import com.jonasfp.paymentservice.ledger.domain.LedgerAccount;
import com.jonasfp.paymentservice.ledger.web.dto.AccountBalanceResponse;
import com.jonasfp.paymentservice.ledger.web.dto.TrialBalanceEntry;
import com.jonasfp.paymentservice.ledger.web.dto.TrialBalanceResponse;
import com.jonasfp.paymentservice.ledger.infra.JournalLineRepository;
import com.jonasfp.paymentservice.ledger.infra.LedgerAccountRepository;

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
        LedgerAccount account =
            ledgerAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Account not found: " + accountId));

        List<JournalLine> lines = journalLineRepository
            .findByLedgerAccountIdAndCreatedAtBefore(accountId, asOf);

        Money balance = null;

        for (JournalLine line : lines) {
            Money lineMoney = line.getMoney();
            if (balance == null) {
                balance = (line.getDirection() == JournalLineType.DEBIT)
                    ? lineMoney
                    : Money.of(BigDecimal.ZERO, lineMoney.currency().value())
                        .minus(lineMoney);
            } else {
                if (line.getDirection() == JournalLineType.DEBIT) {
                    balance = balance.plus(lineMoney);
                } else {
                    balance = balance.minus(lineMoney);
                }
            }
        }

        BigDecimal finalAmount =
            balance != null ? balance.majorAmount() : BigDecimal.ZERO;
        String finalCurrency =
            balance != null ? balance.currency().value() : "USD";

        if ("LIABILITY".equals(account.getAccountType()) ||
            "EQUITY".equals(account.getAccountType()) ||
            "REVENUE".equals(account.getAccountType())) {
            finalAmount = finalAmount.negate();
        }

        return new AccountBalanceResponse(
            account.getId(),
            account.getAccountCode(),
            finalAmount,
            finalCurrency,
            asOf);
    }

    @Transactional(readOnly = true)
    public TrialBalanceResponse getTrialBalance(OffsetDateTime asOf) {
        List<TrialBalanceEntry> entries = journalLineRepository
            .getTrialBalance(asOf);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (TrialBalanceEntry entry : entries) {
            totalDebits = totalDebits.add(entry.totalDebit());
            totalCredits = totalCredits.add(entry.totalCredit());
        }

        Boolean isBalanced = true;

        if (totalDebits.subtract(totalCredits)
            .compareTo(BigDecimal.ZERO) != 0) {
            isBalanced = false;
        }

        return new TrialBalanceResponse(asOf, totalDebits, totalCredits,
            isBalanced, entries);
    }
}
