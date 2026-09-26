package com.jonasfp.paymentservice.reconciliation.application;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.jonasfp.paymentservice.payments.domain.Capture;
import com.jonasfp.paymentservice.payments.domain.Refund;
import com.jonasfp.paymentservice.reconciliation.domain.ProcessorStatementRow;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationBreak;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationBreakType;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRun;
import com.jonasfp.paymentservice.reconciliation.domain.ReconciliationRunStatus;
import com.jonasfp.paymentservice.reconciliation.infra.ProcessorStatementRowRepository;
import com.jonasfp.paymentservice.reconciliation.infra.ReconciliationBreakRepository;
import com.jonasfp.paymentservice.reconciliation.infra.ReconciliationRunRepository;
import com.jonasfp.paymentservice.reconciliation.web.dto.ProcessorStatementCsvRow;
import com.jonasfp.paymentservice.reconciliation.web.dto.ReconciliationRunSummary;
import com.jonasfp.paymentservice.payments.infra.CaptureRepository;
import com.jonasfp.paymentservice.payments.infra.RefundRepository;
import com.jonasfp.paymentservice.domain.Money;
import com.jonasfp.paymentservice.domain.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private final ReconciliationRunRepository runRepository;
    private final ProcessorStatementRowRepository processorStatementRowRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final CaptureRepository captureRepository;
    private final RefundRepository refundRepository;
    private final CsvMapper csvMapper;

    public ReconciliationService(ReconciliationRunRepository runRepository,
        ProcessorStatementRowRepository processorStatementRowRepository,
        ReconciliationBreakRepository breakRepository,
        CaptureRepository captureRepository,
        RefundRepository refundRepository) {
        this.runRepository = runRepository;
        this.processorStatementRowRepository = processorStatementRowRepository;
        this.breakRepository = breakRepository;
        this.captureRepository = captureRepository;
        this.refundRepository = refundRepository;
        this.csvMapper = new CsvMapper();
    }

    @Transactional(readOnly = true)
    public ReconciliationRunSummary getRunSummary(UUID runId) {
        ReconciliationRun run = runRepository.findById(runId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Reconciliation run not found: " + runId));

        List<Object[]> breakCounts = breakRepository.countBreaksByType(runId);
        Map<ReconciliationBreakType, Long> summary = breakCounts.stream()
            .collect(Collectors.toMap(
                row -> (ReconciliationBreakType) row[0],
                row -> (Long) row[1]));

        return new ReconciliationRunSummary(
            run.getId(),
            run.getBusinessDate(),
            run.getStatus(),
            run.getStartedAt(),
            run.getCompletedAt(),
            summary);
    }

    @Transactional
    public UUID importStatement(LocalDate businessDate, MultipartFile file)
        throws IOException {
        // 1. Create the Run
        ReconciliationRun run = new ReconciliationRun();
        run.setBusinessDate(businessDate);
        run.setStatus(ReconciliationRunStatus.PENDING);
        run = runRepository.save(run);

        // 2. Parse CSV
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        try (InputStream is = file.getInputStream()) {
            MappingIterator<ProcessorStatementCsvRow> it = csvMapper
                .readerFor(ProcessorStatementCsvRow.class)
                .with(schema)
                .readValues(is);

            List<ProcessorStatementRow> entities = new ArrayList<>();
            while (it.hasNext()) {
                ProcessorStatementCsvRow csvRow = it.next();
                entities.add(mapToEntity(run, csvRow));
            }

            // 3. Save all rows
            processorStatementRowRepository.saveAll(entities);
        }

        return run.getId();
    }

    @Transactional
    public UUID runReconciliation(LocalDate businessDate) {
        // Find the PENDING run
        ReconciliationRun run = runRepository
            .findWithLockFirstByBusinessDateAndStatus(businessDate,
                ReconciliationRunStatus.PENDING)
            .orElseThrow(() -> new IllegalStateException(
                "No PENDING reconciliation run found for " + businessDate));

        // Update status to RUNNING
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setStartedAt(OffsetDateTime.now());
        run = runRepository.save(run);

        try {
            // Load processor statement rows into a hash table
            Map<String, ProcessorStatementRow> statementRowByRef = 
                processorStatementRowRepository
                    .findAllByReconciliationRunIdAsMap(run.getId());

            OffsetDateTime start =
                businessDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = businessDate.plusDays(1).atStartOfDay()
                .atOffset(ZoneOffset.UTC);

            // NOTE: Adding indexes for date on capture and refund tables would
            //       speed up this code, but at the cost of slower transactions
            List<Capture> captures =
                captureRepository.findAllByCreatedAtBetween(start, end);
            List<Refund> refunds =
                refundRepository.findAllByCreatedAtBetween(start, end);

            // Matching Logic
            List<ReconciliationBreak> breaks = new ArrayList<>();

            for (Capture capture: captures) {
              String ref = capture.getProcessorCaptureReference();
              ProcessorStatementRow statementRow = statementRowByRef.remove(ref);

              if (statementRow != null) {
                if (!capture.getMoney().equals(statementRow.getMoney())) {
                  breaks.add(createAmountMismatchBreak(run, statementRow,
                      capture.getPaymentId(),
                      String.format(
                          "Amount mismatch: Internal=%s, " +
                              "Processor=%s",
                          capture.getMoney(), statementRow.getMoney())));
              }
              } else {
                breaks.add(createMissingProcessorBreak(run,
                  capture.getPaymentId(),
                  "Internal capture record missing from processor "
                      + "statement: "
                      + capture.getProcessorCaptureReference()));
              }
            }

            for (Refund refund: refunds) {
              String ref = refund.getProcessorRefundReference();
              ProcessorStatementRow statementRow = statementRowByRef.remove(ref);

              if (statementRow != null) {
                if (!refund.getMoney().equals(statementRow.getMoney())) {
                  breaks.add(createAmountMismatchBreak(run, statementRow,
                      refund.getPaymentId(),
                      String.format(
                          "Amount mismatch: Internal=%s, " +
                              "Processor=%s",
                          refund.getMoney(), statementRow.getMoney())));
              } else {
                breaks.add(createMissingProcessorBreak(run,
                  refund.getPaymentId(),
                  "Internal refund record missing from processor "
                      + "statement: "
                      + refund.getProcessorRefundReference()));
              }
            }}

            for (ProcessorStatementRow statementRow : statementRowByRef.values()) {
              if (statementRow.getRecordType().equals(TransactionType.CAPTURE)) {
                breaks.add(createMissingInternalBreak(run, statementRow,
                  "No internal capture record found for processor "
                      + "reference: "
                      + statementRow.getProcessorReference()));
              } else if (statementRow.getRecordType().equals(TransactionType.REFUND)) {
                breaks.add(createMissingInternalBreak(run, statementRow,
                  "No internal refund record found for processor " 
                      + "reference: "
                      + statementRow.getProcessorReference()));
              }
            }

            // Save breaks
            breakRepository.saveAll(breaks);

            // Complete run
            run.setStatus(ReconciliationRunStatus.SUCCEEDED);
        } catch (Exception e) {
            run.setStatus(ReconciliationRunStatus.FAILED);
            // NOTE: In a real app, we might log the error details somewhere
        } finally {
            run.setCompletedAt(OffsetDateTime.now());
            runRepository.save(run);
        }

        return run.getId();
    }

    private ReconciliationBreak createMissingInternalBreak(
        ReconciliationRun run, ProcessorStatementRow row, String details) {
        ReconciliationBreak b = new ReconciliationBreak();
        b.setReconciliationRun(run);
        b.setProcessorStatementRow(row);
        b.setBreakType(ReconciliationBreakType.MISSING_INTERNAL_RECORD);
        b.setBreakDetails(details);
        return b;
    }

    private ReconciliationBreak createMissingProcessorBreak(
        ReconciliationRun run, UUID paymentId, String details) {
        ReconciliationBreak b = new ReconciliationBreak();
        b.setReconciliationRun(run);
        b.setPaymentId(paymentId);
        b.setBreakType(ReconciliationBreakType.MISSING_PROCESSOR_RECORD);
        b.setBreakDetails(details);
        return b;
    }

    private ReconciliationBreak createAmountMismatchBreak(ReconciliationRun run,
        ProcessorStatementRow row, UUID paymentId, String details) {
        ReconciliationBreak b = new ReconciliationBreak();
        b.setReconciliationRun(run);
        b.setProcessorStatementRow(row);
        b.setPaymentId(paymentId);
        b.setBreakType(ReconciliationBreakType.AMOUNT_MISMATCH);
        b.setBreakDetails(details);
        return b;
    }

    private ProcessorStatementRow mapToEntity(ReconciliationRun run,
        ProcessorStatementCsvRow csv) {
        ProcessorStatementRow entity = new ProcessorStatementRow();
        entity.setReconciliationRun(run);
        entity.setBusinessDate(LocalDate.parse(csv.businessDate()));
        entity.setRecordType(csv.recordType());
        entity.setProcessorReference(csv.processorReference());
        entity.setMoney(Money.of(csv.amount(), csv.currency()));
        return entity;
    }
}
