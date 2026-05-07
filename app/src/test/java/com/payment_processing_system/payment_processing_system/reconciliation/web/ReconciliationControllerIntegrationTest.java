package com.payment_processing_system.payment_processing_system.reconciliation.web;

import com.payment_processing_system.payment_processing_system.reconciliation.infra.ProcessorStatementRowRepository;
import com.payment_processing_system.payment_processing_system.reconciliation.infra.ReconciliationRunRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ReconciliationControllerIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments")
            .withUsername("payments")
            .withPassword("payments")
            .withInitScript("init-db.sql");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReconciliationRunRepository runRepository;

    @Autowired
    private ProcessorStatementRowRepository rowRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery(
                "TRUNCATE TABLE reconciliation_breaks, processor_statement_rows, reconciliation_runs "
                    +
                    "RESTART IDENTITY CASCADE")
                .executeUpdate();
            return null;
        });
    }

    @Test
    void importStatement_validCsv_createsRunAndRows() {
        // 1. Prepare CSV content
        String csvContent = """
            business_date,record_type,processor_reference,amount,currency
            2026-05-05,CAPTURE,proc_1,100.00,USD
            2026-05-05,REFUND,proc_2,50.00,USD
            """;

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("businessDate", "2026-05-05");
        bodyBuilder.part("file", csvContent.getBytes())
            .header("Content-Disposition",
                "form-data; name=\"file\"; filename=\"statement.csv\"")
            .contentType(MediaType.TEXT_PLAIN);

        // 2. Call the endpoint
        webTestClient.post()
            .uri("/v1/payments/reconciliation/imports")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(bodyBuilder.build())
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.runId").exists()
            .consumeWith(result -> {
                String body = new String(result.getResponseBodyContent());
                UUID runId = UUID.fromString(body
                    .substring(body.indexOf(":") + 2, body.indexOf("}") - 1));

                // 3. Verify database state
                assertThat(runRepository.existsById(runId)).isTrue();
                assertThat(rowRepository.findAll()).hasSize(2);

                var rows = rowRepository.findAll();
                assertThat(rows).anySatisfy(row -> {
                    assertThat(row.getProcessorReference()).isEqualTo("proc_1");
                    assertThat(row.getAmount()).isEqualByComparingTo("100.00");
                    assertThat(row.getRecordType()).isEqualTo("CAPTURE");
                });
                assertThat(rows).anySatisfy(row -> {
                    assertThat(row.getProcessorReference()).isEqualTo("proc_2");
                    assertThat(row.getAmount()).isEqualByComparingTo("50.00");
                    assertThat(row.getRecordType()).isEqualTo("REFUND");
                });
            });
    }

    @Test
    void runReconciliation_withoutBreaks_matchesAllRecords() {
        LocalDate businessDate = LocalDate.now();
        String idempotencyKey = UUID.randomUUID().toString();

        // 1. Create internal record (Authorize + Capture)
        AuthorizePaymentRequest authRequest = new AuthorizePaymentRequest(
            "cust-1", UUID.randomUUID(), new BigDecimal("10000"), "USD");
        webTestClient.post().uri("/v1/payments/authorize")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON).bodyValue(authRequest)
            .exchange().expectStatus().isCreated();

        CapturePaymentRequest capRequest =
            new CapturePaymentRequest("cust-1", new BigDecimal("10000"), "USD");
        webTestClient.post()
            .uri("/v1/payments/{id}/capture", getPaymentId(idempotencyKey))
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON).bodyValue(capRequest)
            .exchange().expectStatus().isCreated();

        // 2. Import matching processor statement
        String csvContent = String.format("""
            business_date,record_type,processor_reference,amount,currency
            %s,CAPTURE,cap_%s,100.00,USD
            """, businessDate, idempotencyKey.substring(0, 8));

        importCsv(businessDate, csvContent);

        // 3. Run reconciliation
        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/v1/payments/reconciliation/runs")
                .queryParam("businessDate", businessDate.toString()).build())
            .exchange().expectStatus().isCreated();

        // 4. Verify results
        UUID runId = runRepository
            .findByBusinessDateAndStatus(businessDate,
                ReconciliationRunStatus.SUCCEEDED)
            .orElseThrow().getId();

        webTestClient.get()
            .uri("/v1/payments/reconciliation/runs/{runId}", runId)
            .exchange().expectStatus().isOk()
            .expectBody(ReconciliationRunSummary.class)
            .consumeWith(result -> {
                ReconciliationRunSummary summary = result.getResponseBody();
                assertThat(summary.status())
                    .isEqualTo(ReconciliationRunStatus.SUCCEEDED);
                assertThat(summary.breakSummary()).isEmpty();
            });

        assertThat(breakRepository.count()).isEqualTo(0);
    }

    private UUID getPaymentId(String idempotencyKey) {
        return (UUID) entityManager.createNativeQuery(
            "SELECT resource_id FROM idempotency_keys " +
                "WHERE idempotency_key = :key AND action_type = 'AUTHORIZE'")
            .setParameter("key", idempotencyKey)
            .getSingleResult();
    }

    private void importCsv(LocalDate businessDate, String csvContent) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("businessDate", businessDate.toString());
        bodyBuilder.part("file", csvContent.getBytes())
            .header("Content-Disposition",
                "form-data; name=\"file\"; filename=\"statement.csv\"")
            .contentType(MediaType.TEXT_PLAIN);

        webTestClient.post().uri("/v1/payments/reconciliation/imports")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(bodyBuilder.build())
            .exchange().expectStatus().isCreated();
    }
}
