CREATE TABLE captures (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    payment_event_id UUID NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    processor_capture_reference VARCHAR(128),
    journal_entry_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_captures_payment 
        FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_captures_payment_event
        FOREIGN KEY (payment_event_id) REFERENCES payment_events (id),
    -- TODO: Link to journal entry table when it is created
    
    CONSTRAINT chk_capture_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_no_micro_cents_captured
        CHECK (amount * 100 = TRUNC(amount * 100)),

    CONSTRAINT chk_valid_currency_code CHECK (currency IN ('USD', 'CAD'))
);

-- Prevent the payments_app from changing anything it shouldn't
GRANT SELECT ON captures TO payments_app; 

GRANT INSERT (
        id, payment_id, payment_event_id, amount, currency, 
        processor_capture_reference
    ) ON captures TO payments_app;

GRANT UPDATE (
        processor_capture_reference
    ) ON captures TO payments_app;

-- Prevent anyone from deleting a capture record
CREATE OR REPLACE FUNCTION block_capture_deletions()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Deletions are not permitted on the captures table.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_no_delete_captures
BEFORE DELETE ON captures
FOR EACH ROW EXECUTE FUNCTION block_capture_deletions();

-- Ensure that capture currency always matches its payment's currency
CREATE OR REPLACE FUNCTION validate_capture_currency_matches_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM payments p
        WHERE p.id = NEW.payment_id
          AND p.currency <> NEW.currency
    ) THEN
        RAISE EXCEPTION 'capture currency must match payment currency';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_captures_currency_matches_payment
BEFORE INSERT OR UPDATE ON captures
FOR EACH ROW
EXECUTE FUNCTION validate_capture_currency_matches_payment();

-- Ensure captures can only reference capture-success events
CREATE OR REPLACE FUNCTION validate_capture_success_event()
RETURNS TRIGGER AS $$
DECLARE
    v_event_type VARCHAR(32);
BEGIN
    SELECT pe.event_type
    INTO v_event_type
    FROM payment_events pe
    WHERE pe.id = NEW.payment_event_id;

    IF v_event_type IS DISTINCT FROM 'CAPTURE_SUCCESS' THEN
        RAISE EXCEPTION 'Capture rows must reference a CAPTURE_SUCCESS '
        ' payment event';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_captures_require_capture_success_event
BEFORE INSERT OR UPDATE ON captures
FOR EACH ROW EXECUTE FUNCTION validate_capture_success_event();

-- Ensure the payment's captured amount and status gets auto updated
CREATE OR REPLACE FUNCTION update_payment_captured_amount()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.payments
    SET captured_amount = captured_amount + NEW.amount,
        status = 'FULLY_CAPTURED'
    WHERE id = NEW.payment_id
        AND (captured_amount + NEW.amount) = authorized_amount;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Only full captures are allowed or payment not found';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp;

CREATE TRIGGER trg_after_capture_insert
AFTER INSERT ON captures
FOR EACH ROW EXECUTE FUNCTION update_payment_captured_amount();

REVOKE EXECUTE ON FUNCTION update_payment_captured_amount() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION update_payment_captured_amount() TO payments_app;

-- Ensure that each capture has a corresponding journal entry
CREATE OR REPLACE FUNCTION validate_capture_has_journal_entry()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM journal_entries 
        WHERE capture_id = NEW.id
    ) THEN
        RAISE EXCEPTION 'Capture must have a journal entry.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_check_capture_has_journal_entry
AFTER INSERT ON captures
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_capture_has_journal_entry();

-- Prevent the same payment event from causing multiple captures and speed 
-- up lookups by payment event id
CREATE UNIQUE INDEX uk_captures_payment_event_id
    ON captures (payment_event_id);

-- Speed up lookups by payment id
CREATE INDEX idx_captures_payment_id
    ON captures (payment_id);

-- Prevent two captures from having the same processor capture reference
CREATE UNIQUE INDEX uk_captures_processor_capture_reference
    ON captures (processor_capture_reference)
    WHERE processor_capture_reference IS NOT NULL;