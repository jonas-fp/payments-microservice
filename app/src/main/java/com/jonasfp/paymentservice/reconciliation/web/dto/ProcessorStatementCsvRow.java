package com.jonasfp.paymentservice.reconciliation.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jonasfp.paymentservice.domain.TransactionType;
import java.math.BigDecimal;

public record ProcessorStatementCsvRow(
    @JsonProperty("business_date") String businessDate,
    @JsonProperty("record_type") TransactionType recordType,
    @JsonProperty("processor_reference") String processorReference,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency) {
}
