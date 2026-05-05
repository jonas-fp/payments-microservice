package com.payment_processing_system.payment_processing_system.reconciliation.domain;

import com.payment_processing_system.payment_processing_system.entity.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "processor_statement_rows")
public class ProcessorStatementRow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "record_type", nullable = false, length = 32)
    private String recordType;

    @Column(name = "processor_reference", nullable = false, length = 128)
    private String processorReference;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    public ProcessorStatementRow() {}

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public void setReconciliationRun(ReconciliationRun reconciliationRun) {
        this.reconciliationRun = reconciliationRun;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public void setProcessorReference(String processorReference) {
        this.processorReference = processorReference;
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
