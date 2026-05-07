package com.payment_processing_system.payment_processing_system.reconciliation.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcessorStatementCsvRow(
    @JsonProperty("business_date") String businessDate,
    @JsonProperty("record_type") String recordType,
    @JsonProperty("processor_reference") String processorReference,
    @JsonProperty("amount") BigDecimal amount,
    @JsonProperty("currency") String currency) {
}
