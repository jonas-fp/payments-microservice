package com.payment_processing_system.payment_processing_system.reconciliation.domain;

import com.payment_processing_system.payment_processing_system.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

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

    @Column(name = "break_type", nullable = false, length = 64)
    private String breakType;

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

    public String getBreakType() {
        return breakType;
    }

    public void setBreakType(String breakType) {
        this.breakType = breakType;
    }

    public String getBreakDetails() {
        return breakDetails;
    }

    public void setBreakDetails(String breakDetails) {
        this.breakDetails = breakDetails;
    }
}
