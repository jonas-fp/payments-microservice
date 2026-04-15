CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    action_type VARCHAR(128) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_body JSONB,
    event_id UUID,
    resource_type VARCHAR(64) NOT NULL, 
    resource_id UUID, 
    response_code INTEGER,
    response_status VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    
    CONSTRAINT uk_idempotency_keys_scope_key UNIQUE (
        customer_id, idempotency_key, action_type
    ),
    CONSTRAINT chk_action_type CHECK (
        action_type in ('AUTHORIZE', 'CAPTURE', 'REFUND')
    ),
    CONSTRAINT chk_response_status CHECK (
        response_status in ('STARTED', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT chk_resource_type CHECK (
        resource_type in ('PAYMENT', 'REFUND')
    ),
    CONSTRAINT chk_idempotency_keys_response_code CHECK (
        response_code IS NULL OR (response_code BETWEEN 100 AND 599)
    ),
    CONSTRAINT chk_completed_has_response CHECK (
        (response_status = 'COMPLETED' AND response_code IS NOT NULL AND 
        response_body IS NOT NULL) OR 
        (response_status IN ('STARTED', 'FAILED'))
    )
);

GRANT SELECT ON idempotency_keys TO payments_app;

GRANT INSERT (
    action_type, customer_id, idempotency_key, request_hash, response_body,
    event_id, resource_type, resource_id, response_code, response_status
) ON idempotency_keys TO payments_app;

GRANT UPDATE (
    event_id, resource_id, response_code, response_status
) ON idempotency_keys TO payments_app;