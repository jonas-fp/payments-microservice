package com.payment_processing_system.payment_processing_system.reconciliation.application;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ProcessorStatementRow;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRun;
import com.payment_processing_system.payment_processing_system.reconciliation.domain.ReconciliationRunStatus;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ProcessorStatementRowRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ReconciliationRunRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.web.dto.ProcessorStatementCsvRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ReconciliationRunRepository runRepository;
    private final ProcessorStatementRowRepository rowRepository;
    private final CsvMapper csvMapper;

    public ReconciliationService(ReconciliationRunRepository runRepository,
        ProcessorStatementRowRepository rowRepository) {
        this.runRepository = runRepository;
        this.rowRepository = rowRepository;
        this.csvMapper = new CsvMapper();
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
            rowRepository.saveAll(entities);
        }

        return run.getId();
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
