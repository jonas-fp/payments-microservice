CREATE TABLE payments (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(128) NOT NULL,
    invoice_id UUID NOT NULL, -- Billing system's ID
    authorized_amount NUMERIC(15,2) NOT NULL,
    captured_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(15,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processor_payment_reference VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_payments_authorized_amount_non_negative CHECK (
        authorized_amount >= 0
    ),
    CONSTRAINT chk_payments_captured_amount_non_negative CHECK (
        captured_amount >= 0
    ),
    CONSTRAINT chk_payments_refunded_amount_non_negative CHECK (
        refunded_amount >= 0
    ),

    CONSTRAINT chk_no_micro_cents_authorized
    CHECK (authorized_amount * 100 = TRUNC(authorized_amount * 100)),
    CONSTRAINT chk_no_micro_cents_captured
    CHECK (captured_amount * 100 = TRUNC(captured_amount * 100)),
    CONSTRAINT chk_no_micro_cents_refunded
    CHECK (refunded_amount * 100 = TRUNC(refunded_amount * 100)),
    
    CONSTRAINT chk_valid_currency_code CHECK (currency IN ('USD', 'CAD')),

    CONSTRAINT chk_payments_status CHECK (
        status IN (
            'AUTHORIZED', 'CAPTURED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED',
            'VOIDED'
            )
    ),
    CONSTRAINT chk_payments_captured_less_than_or_equal_to_authorized CHECK (
        captured_amount <= authorized_amount
    ),
    CONSTRAINT chk_payments_refunded_less_than_or_equal_to_captured CHECK (
        refunded_amount <= captured_amount
    )
);

-- Prevent the payments_app from changing anything it shouldn't
GRANT SELECT ON payments TO payments_app; 

GRANT INSERT (
        id, customer_id, invoice_id, authorized_amount, currency, status, 
        processor_payment_reference
    ) ON payments TO payments_app;

GRANT UPDATE (
        processor_payment_reference, status
    ) ON payments TO payments_app;

-- Prevent anyone from deleting a payment record
CREATE OR REPLACE FUNCTION block_payment_deletions()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Deletions are not permitted on the payments table.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_no_delete_payments
BEFORE DELETE ON payments
FOR EACH ROW EXECUTE FUNCTION block_payment_deletions();

-- Prevent multiple payments with the same processor reference
CREATE UNIQUE INDEX uk_payments_processor_payment_reference
    ON payments (processor_payment_reference)
    WHERE processor_payment_reference IS NOT NULL;

-- Allow users to quickly look up payments by invoice
CREATE INDEX idx_payments_invoice_id
    ON payments (invoice_id);