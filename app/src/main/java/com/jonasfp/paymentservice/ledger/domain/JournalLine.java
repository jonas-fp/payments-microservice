package com.jonasfp.paymentservice.ledger.domain;

import java.math.BigDecimal;
import java.util.UUID;
import com.jonasfp.paymentservice.domain.JournalLineType;
import com.jonasfp.paymentservice.domain.Money;
import com.jonasfp.paymentservice.infra.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "journal_lines")
public class JournalLine extends BaseEntity {

    @Column(name = "journal_entry_id", nullable = false)
    private UUID journalEntryId;

    @Column(name = "ledger_account_id", nullable = false)
    private UUID ledgerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private JournalLineType direction;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    public JournalLine() {
    }

    public Money getMoney() {
        return Money.of(amount, currency);
    }

    public void setMoney(Money money) {
        this.amount = money.majorAmount();
        this.currency = money.currency().value();
    }

    public UUID getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(UUID journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public UUID getLedgerAccountId() {
        return ledgerAccountId;
    }

    public void setLedgerAccountId(UUID ledgerAccountId) {
        this.ledgerAccountId = ledgerAccountId;
    }

    public JournalLineType getDirection() {
        return direction;
    }

    public void setDirection(JournalLineType direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
