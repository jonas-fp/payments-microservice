-- NOTE: reconciliation is modeled at the payment_id level rather than the
--       capture/refund level. This may need to be changed in the future, as I
--       learn more about how to work with payment processors.
CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY,
    business_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reconciliation_runs_status CHECK (
        status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')
    ),
    CONSTRAINT chk_reconciliation_runs_status_timestamps CHECK (
        (status = 'PENDING' AND started_at IS NULL AND completed_at IS NULL)
        OR (
            status = 'RUNNING' AND started_at IS NOT NULL AND
            completed_at IS NULL)
        OR (
            status IN ('SUCCEEDED', 'FAILED')
            AND started_at IS NOT NULL
            AND completed_at IS NOT NULL
            AND completed_at >= started_at
        )
    )
);

GRANT SELECT ON reconciliation_runs TO payments_app;

GRANT INSERT (
        id, business_date, status, started_at, completed_at
    ) ON reconciliation_runs TO payments_app;

GRANT UPDATE (
        status, started_at, completed_at
    ) ON reconciliation_runs TO payments_app;

CREATE TABLE processor_statement_rows (
    id UUID PRIMARY KEY,
    reconciliation_run_id UUID NOT NULL,
    business_date DATE NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    processor_reference VARCHAR(128) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_processor_statement_rows_reconciliation_run
        FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs (id),
    CONSTRAINT chk_processor_statement_rows_record_type CHECK (
        record_type IN ('CAPTURE', 'REFUND')
    ),
    CONSTRAINT chk_processor_statement_rows_amount_non_negative 
        CHECK (amount >= 0),
    CONSTRAINT chk_processor_statement_rows_currency
        CHECK (currency IN ('USD', 'CAD'))
);

GRANT SELECT ON processor_statement_rows TO payments_app;

GRANT INSERT (
        id, reconciliation_run_id, business_date, record_type, 
        processor_reference, amount, currency
    ) ON processor_statement_rows TO payments_app;

CREATE TABLE reconciliation_breaks (
    id UUID PRIMARY KEY,
    reconciliation_run_id UUID NOT NULL,
    processor_statement_row_id UUID,
    payment_id UUID,
    break_type VARCHAR(64) NOT NULL,
    break_details TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_reconciliation_breaks_reconciliation_run
        FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs (id),
    CONSTRAINT fk_reconciliation_breaks_processor_statement_row
        FOREIGN KEY (processor_statement_row_id) 
        REFERENCES processor_statement_rows (id),
    CONSTRAINT fk_reconciliation_breaks_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT chk_reconciliation_breaks_type CHECK (
        break_type IN (
            'MISSING_INTERNAL_RECORD',
            'MISSING_PROCESSOR_RECORD',
            'DUPLICATE_INTERNAL_RECORD',
            'DUPLICATE_PROCESSOR_RECORD',
            'AMOUNT_MISMATCH'
        )
    ),
    CONSTRAINT chk_reconciliation_breaks_references CHECK (
        (
            break_type = 'MISSING_INTERNAL_RECORD' AND 
            processor_statement_row_id IS NOT NULL AND
            payment_id IS NULL
        )
        OR (
            break_type = 'MISSING_PROCESSOR_RECORD' AND 
            payment_id IS NOT NULL
            AND processor_statement_row_id IS NULL
        )
        OR (
            break_type = 'DUPLICATE_INTERNAL_RECORD' AND 
            processor_statement_row_id IS NOT NULL AND
            payment_id IS NULL
        )
        OR (
            break_type = 'DUPLICATE_PROCESSOR_RECORD' AND 
            payment_id IS NOT NULL AND
            processor_statement_row_id IS NULL
        )
        OR (
            break_type = 'AMOUNT_MISMATCH' AND 
            processor_statement_row_id IS NOT NULL 
            AND payment_id IS NOT NULL
        )
    )
);

GRANT SELECT ON reconciliation_breaks TO payments_app;
GRANT INSERT (
        id, reconciliation_run_id, processor_statement_row_id, payment_id, 
        break_type, break_details
    ) ON reconciliation_breaks TO payments_app;

CREATE INDEX idx_processor_statement_rows_reconciliation_run_id
    ON processor_statement_rows (reconciliation_run_id);

CREATE INDEX idx_processor_statement_rows_processor_reference
    ON processor_statement_rows (processor_reference);

CREATE INDEX idx_reconciliation_breaks_reconciliation_run_id
    ON reconciliation_breaks (reconciliation_run_id);