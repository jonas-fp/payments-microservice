CREATE TABLE idempotency_keys (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action_type VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64),
    resource_id UUID,
    response_status INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- DESIGN: ensures that any action on the same endpoint with the same 
    -- idempotency key is only executed once.
    CONSTRAINT uk_idempotency_keys_scope_key UNIQUE (
        endpoint_scope, idempotency_key
    ),
    CONSTRAINT chk_resource_type CHECK (
        resource_type in ('AUTHORIZE', 'CAPTURE', 'REFUND')
    ),
    CONSTRAINT chk_idempotency_keys_response_status CHECK (
        response_status IS NULL OR (response_status BETWEEN 100 AND 599)
    )
);