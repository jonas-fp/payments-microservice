package com.payment_processing_system.payment_processing_system.reconciliation.application;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.payment_processing_system.payment_processing_system.entity.CaptureEntity;
import com.payment_processing_system.payment_processing_system.entity.RefundEntity;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ProcessorStatementRow;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationBreak;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRun;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRunStatus;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ProcessorStatementRowRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ReconciliationBreakRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ReconciliationRunRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.web.dto.ProcessorStatementCsvRow;
import com.payment_processing_system.payment_processing_system.reconciliation.web.dto.ReconciliationRunSummary;
import com.payment_processing_system.payment_processing_system.repository.CaptureRepository;
import com.payment_processing_system.payment_processing_system.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
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
        Map<String, Long> summary = breakCounts.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
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
        // 1. Find the PENDING run
        ReconciliationRun run = runRepository
            .findByBusinessDateAndStatus(businessDate,
                ReconciliationRunStatus.PENDING)
            .orElseThrow(() -> new IllegalStateException(
                "No PENDING reconciliation run found for " + businessDate));

        // 2. Update status to RUNNING
        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setStartedAt(OffsetDateTime.now());
        run = runRepository.save(run);

        try {
            // 3. Fetch data
            List<ProcessorStatementRow> statementRows =
                processorStatementRowRepository
                    .findAllByReconciliationRunId(run.getId());

            OffsetDateTime start =
                businessDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime end = businessDate.plusDays(1).atStartOfDay()
                .atOffset(ZoneOffset.UTC);

            List<CaptureEntity> captures =
                captureRepository.findAllByCreatedAtBetween(start, end);
            List<RefundEntity> refunds =
                refundRepository.findAllByCreatedAtBetween(start, end);

            // 4. Matching Logic
            Set<UUID> matchedCaptureIds = new HashSet<>();
            Set<UUID> matchedRefundIds = new HashSet<>();
            List<ReconciliationBreak> breaks = new ArrayList<>();

            for (ProcessorStatementRow row : statementRows) {
                if ("CAPTURE".equals(row.getRecordType())) {
                    Optional<CaptureEntity> captureOpt = captures.stream()
                        .filter(c -> c.getProcessorCaptureReference()
                            .equals(row.getProcessorReference()))
                        .findFirst();

                    if (captureOpt.isPresent()) {
                        CaptureEntity capture = captureOpt.get();
                        matchedCaptureIds.add(capture.getId());
                        if (capture.getAmount()
                            .compareTo(row.getAmount()) != 0) {
                            breaks.add(createAmountMismatchBreak(run, row,
                                capture.getPaymentId(),
                                String.format(
                                    "Amount mismatch: Internal=%s, " +
                                        "Processor=%s",
                                    capture.getAmount(), row.getAmount())));
                        }
                    } else {
                        breaks.add(createMissingInternalBreak(run, row,
                            "No internal capture record found for processor "
                                + "reference: "
                                + row.getProcessorReference()));
                    }
                } else if ("REFUND".equals(row.getRecordType())) {
                    Optional<RefundEntity> refundOpt = refunds.stream()
                        .filter(r -> r.getProcessorRefundReference()
                            .equals(row.getProcessorReference()))
                        .findFirst();

                    if (refundOpt.isPresent()) {
                        RefundEntity refund = refundOpt.get();
                        matchedRefundIds.add(refund.getId());
                        if (refund.getAmount()
                            .compareTo(row.getAmount()) != 0) {
                            breaks.add(createAmountMismatchBreak(run, row,
                                refund.getPaymentId(),
                                String.format(
                                    "Amount mismatch: Internal=%s, " +
                                        "Processor=%s",
                                    refund.getAmount(), row.getAmount())));
                        }
                    } else {
                        breaks.add(createMissingInternalBreak(run, row,
                            "No internal refund record found for processor " +
                                "reference: "
                                + row.getProcessorReference()));
                    }
                }
            }

            // 5. Identify Missing Processor Records
            for (CaptureEntity capture : captures) {
                if (!matchedCaptureIds.contains(capture.getId())) {
                    breaks.add(createMissingProcessorBreak(run,
                        capture.getPaymentId(),
                        "Internal capture record missing from processor "
                            + "statement: "
                            + capture.getProcessorCaptureReference()));
                }
            }
            for (RefundEntity refund : refunds) {
                if (!matchedRefundIds.contains(refund.getId())) {
                    breaks.add(createMissingProcessorBreak(run,
                        refund.getPaymentId(),
                        "Internal refund record missing from processor "
                            + "statement: "
                            + refund.getProcessorRefundReference()));
                }
            }

            // 6. Save breaks
            breakRepository.saveAll(breaks);

            // 7. Complete run
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
        b.setBreakType("MISSING_INTERNAL_RECORD");
        b.setBreakDetails(details);
        return b;
    }

    private ReconciliationBreak createMissingProcessorBreak(
        ReconciliationRun run, UUID paymentId, String details) {
        ReconciliationBreak b = new ReconciliationBreak();
        b.setReconciliationRun(run);
        b.setPaymentId(paymentId);
        b.setBreakType("MISSING_PROCESSOR_RECORD");
        b.setBreakDetails(details);
        return b;
    }

    private ReconciliationBreak createAmountMismatchBreak(ReconciliationRun run,
        ProcessorStatementRow row, UUID paymentId, String details) {
        ReconciliationBreak b = new ReconciliationBreak();
        b.setReconciliationRun(run);
        b.setProcessorStatementRow(row);
        b.setPaymentId(paymentId);
        b.setBreakType("AMOUNT_MISMATCH");
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
        entity.setAmount(csv.amount());
        entity.setCurrency(csv.currency());
        return entity;
    }
}
