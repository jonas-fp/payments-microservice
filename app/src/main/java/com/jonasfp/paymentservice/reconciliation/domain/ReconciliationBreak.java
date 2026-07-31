package com.jonasfp.paymentservice.reconciliation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.UUID;
import com.jonasfp.paymentservice.infra.persistence.BaseEntity;

@Entity
@Table(name = "reconciliation_breaks")
public class ReconciliationBreak extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRun reconciliationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_statement_row_id")
    private ProcessorStatementRow processorStatementRow;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "break_type", nullable = false, length = 64)
    private ReconciliationBreakType breakType;

    @Column(name = "break_details", nullable = false, columnDefinition = "TEXT")
    private String breakDetails;

    public ReconciliationBreak() {}

    public ReconciliationRun getReconciliationRun() {
        return reconciliationRun;
    }

    public void setReconciliationRun(ReconciliationRun reconciliationRun) {
        this.reconciliationRun = reconciliationRun;
    }

    public ProcessorStatementRow getProcessorStatementRow() {
        return processorStatementRow;
    }

    public void setProcessorStatementRow(
        ProcessorStatementRow processorStatementRow) {
        this.processorStatementRow = processorStatementRow;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public ReconciliationBreakType getBreakType() {
        return breakType;
    }

    public void setBreakType(ReconciliationBreakType breakType) {
        this.breakType = breakType;
    }

    public String getBreakDetails() {
        return breakDetails;
    }

    public void setBreakDetails(String breakDetails) {
        this.breakDetails = breakDetails;
    }
}
