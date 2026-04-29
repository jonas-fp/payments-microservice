package com.payment_processing_system.payment_processing_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_accounts")
public class LedgerAccountEntity extends BaseEntity {

    @Column(name = "account_code", nullable = false, unique = true, length = 64)
    private String accountCode;

    @Column(name = "account_name", nullable = false, length = 128)
    private String accountName;

    @Column(name = "account_type", nullable = false, length = 32)
    private String accountType;

    public LedgerAccountEntity() {
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
