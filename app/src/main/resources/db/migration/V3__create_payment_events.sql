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

CREATE INDEX idx_payment_events_payment_id 
    ON payment_events(payment_id);