CREATE TABLE payments (
    internal_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id UUID NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    billing_reference_id UUID NOT NULL, -- billing system's id
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
            'AUTHORIZED', 'CAPTURED', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED'
            )
    ),
    CONSTRAINT chk_payments_captured_less_than_or_equal_to_authorized CHECK (
        captured_amount <= authorized_amount
    ),
    CONSTRAINT chk_payments_refunded_less_than_or_equal_to_captured CHECK (
        refunded_amount <= captured_amount
    )
);

-- Permissions for immutability
GRANT SELECT ON payments TO payments_user;

GRANT INSERT (
        public_id, customer_id, billing_reference_id, authorized_amount, 
        captured_amount, refunded_amount, currency, status,
        processor_payment_reference
    ) ON payments TO payments_user,

GRANT UPDATE (
        authorized_amount, captured_amount, refunded_amount, currency, status,
    ) ON payments TO payments_user,

GRANT USAGE, SELECT ON SEQUENCE payments_internal_id_seq TO payments_user;
CREATE UNIQUE INDEX uk_payments_processor_payment_reference
    ON payments (processor_payment_reference)
    WHERE processor_payment_reference IS NOT NULL;

CREATE INDEX uk_payments_billing_reference_id
    ON payments (billing_reference_id);