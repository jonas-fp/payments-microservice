CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    processor_event_reference VARCHAR(128),
    processor_response JSONB,
    idempotency_key_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_events_payment 
        FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_events_idempotency_key 
        FOREIGN KEY (idempotency_key_id) REFERENCES idempotency_keys (id),

    CONSTRAINT chk_payment_events_event_type CHECK (
        event_type IN (
        'AUTHORIZE_REQUESTED', 'AUTHORIZE_SUCCESS', 'AUTHORIZE_FAILED',
        'AUTHORIZE_VOIDED',
        'CAPTURE_REQUESTED', 'CAPTURE_SUCCESS', 'CAPTURE_FAILED',
        'CAPTURE_VOIDED',
        'REFUND_REQUESTED', 'REFUND_SUCCESS', 'REFUND_FAILED',
        'REFUND_VOIDED'
    )
    )
);

GRANT SELECT ON payment_events TO payments_app;

GRANT INSERT (
    payment_id, event_type, processor_event_reference, processor_response,
    idempotency_key_id
) ON payment_events TO payments_app;

CREATE UNIQUE INDEX uk_payment_events_processor_event_reference
    ON payment_events (processor_event_reference)
    WHERE processor_event_reference IS NOT NULL;

CREATE INDEX idx_payment_events_payment_id 
    ON payment_events(payment_id);

-- Ensure that capture and refund success events always have a corresponding
-- record in the captures or refunds table
CREATE OR REPLACE FUNCTION validate_event_has_capture_or_refund_record()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.event_type = 'CAPTURE_SUCCESS') THEN
        IF NOT EXISTS (SELECT 1 FROM captures WHERE event_id = NEW.id) THEN
            RAISE EXCEPTION 'CAPTURE_SUCCESS event must have a corresponding ' 
                            'record in captures table.';
        END IF;
    ELSIF (NEW.event_type = 'REFUND_SUCCESS') THEN
        IF NOT EXISTS (SELECT 1 FROM refunds WHERE event_id = NEW.id) THEN
            RAISE EXCEPTION 'REFUND_SUCCESS event must have a corresponding ' 
                            'record in refunds table.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_check_event_has_capture_or_refund_record
AFTER INSERT ON payment_events
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_event_has_capture_or_refund_record();

-- Ensure that the audit trail is immutable
CREATE OR REPLACE FUNCTION block_payment_event_deletions_and_updates()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Deletions are not permitted on the payment_events '
    ' table.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_no_delete_payment_events
BEFORE UPDATE OR DELETE ON payment_events
FOR EACH ROW EXECUTE FUNCTION block_payment_event_deletions_and_updates();