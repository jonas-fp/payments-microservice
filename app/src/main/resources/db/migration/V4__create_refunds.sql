CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL, 
    payment_event_id UUID NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    processor_refund_reference VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_refunds_payment 
        FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_refunds_payment_event
        FOREIGN KEY (payment_event_id) REFERENCES payment_events (id),
    
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_no_micro_cents_refunded
        CHECK (amount * 100 = TRUNC(amount * 100)),

    CONSTRAINT chk_valid_currency_code CHECK (currency IN ('USD', 'CAD'))
);

-- Prevent the payments_app from changing anything it shouldn't
GRANT SELECT ON refunds TO payments_app; 

GRANT INSERT (
        id, payment_id, payment_event_id, amount, currency, 
        processor_refund_reference
    ) ON refunds TO payments_app;

GRANT UPDATE (
        processor_refund_reference
    ) ON refunds TO payments_app;

-- Ensure that refund currency always matches its payment's currency
CREATE OR REPLACE FUNCTION validate_refund_currency_matches_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM payments p
        WHERE p.id = NEW.payment_id
          AND p.currency <> NEW.currency
    ) THEN
        RAISE EXCEPTION 'refund currency must match payment currency';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_refunds_currency_matches_payment
BEFORE INSERT OR UPDATE ON refunds
FOR EACH ROW
EXECUTE FUNCTION validate_refund_currency_matches_payment();

-- Prevent the same payment event from causing multiple refunds
CREATE UNIQUE INDEX uk_refunds_payment_event_id
    ON refunds (payment_event_id);

-- Prevent two refunds from having the same processor refund reference
CREATE UNIQUE INDEX uk_refunds_processor_refund_reference
    ON refunds (processor_refund_reference)
    WHERE processor_refund_reference IS NOT NULL;