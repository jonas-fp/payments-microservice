CREATE TABLE payment_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, 
    payment_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    processor_event_reference VARCHAR(128),
    processor_response JSONB,
    journal_entry_id BIGINT,
    idempotency_key_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payment_events_payment 
        FOREIGN KEY (payment_id) REFERENCES payments (internal_id),
    CONSTRAINT fk_payment_events_idempotency_key 
        FOREIGN KEY (idempotency_key_id) REFERENCES idempotency_keys (id),

    CONSTRAINT chk_payment_events_event_type CHECK (
        event_type IN (
        'AUTHORIZE_REQUESTED', 'AUTHORIZE_SUCCESS', 'AUTHORIZE_FAILED',
        'CAPTURE_REQUESTED', 'CAPTURE_SUCCESS', 'CAPTURE_FAILED',
        'REFUND_REQUESTED', 'REFUND_SUCCESS', 'REFUND_FAILED'
    )
    )
);

GRANT SELECT ON payment_events TO payments_user;

GRANT INSERT (
    payment_id, event_type, processor_event_reference, processor_response,
     journal_entry_id, idempotency_key_id
) ON payment_events TO payments_user;

GRANT USAGE, SELECT ON SEQUENCE payment_events_id_seq TO payments_user;

CREATE INDEX idx_payment_events_payment_id 
    ON payment_events(payment_id);