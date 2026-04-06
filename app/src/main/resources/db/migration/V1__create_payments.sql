CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(128) NOT NULL,
    authorized_amount NUMERIC(19,4) NOT NULL,
    captured_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    refunded_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processor_payment_reference VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_authorized_amount_non_negative CHECK (authorized_amount >= 0),
    CONSTRAINT chk_payments_captured_amount_non_negative CHECK (captured_amount >= 0),
    CONSTRAINT chk_payments_refunded_amount_non_negative CHECK (refunded_amount >= 0),
    CONSTRAINT chk_payments_currency_uppercase CHECK (currency = UPPER(currency)),
    CONSTRAINT chk_payments_status CHECK (
        status IN ('AUTHORIZED', 'CAPTURED', 'PARTIALLY_REFUNDED', 'REFUNDED')
    ),
    CONSTRAINT chk_payments_captured_less_than_or_equal_to_authorized CHECK (
        captured_amount <= authorized_amount
    ),
    CONSTRAINT chk_payments_refunded_less_than_or_equal_to_captured CHECK (
        refunded_amount <= captured_amount
    )
);

CREATE UNIQUE INDEX uk_payments_processor_payment_reference
    ON payments (processor_payment_reference)
    WHERE processor_payment_reference IS NOT NULL;
