CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY,
    account_code VARCHAR(5) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ledger_accounts_account_code
        UNIQUE (account_code),
    CONSTRAINT chk_ledger_accounts_account_code_length
        CHECK (LENGTH(account_code) = 5),
    CONSTRAINT chk_ledger_accounts_account_type
        CHECK (account_type IN 
            ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'))
);

-- GRANT SELECT ON ledger_accounts TO payments_app;

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    refund_id UUID,
    capture_id UUID,
    transaction_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_journal_entries_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_journal_entries_refund
        FOREIGN KEY (refund_id) REFERENCES refunds (id),
    CONSTRAINT fk_journal_entries_capture
        FOREIGN KEY (capture_id) REFERENCES captures (id),
    CONSTRAINT chk_journal_entries_transaction_type_valid
        CHECK (transaction_type IN ('CAPTURE', 'REFUND')),
    CONSTRAINT chk_journal_entries_transaction_type_xor
        CHECK (
            (refund_id IS NULL AND capture_id IS NOT NULL AND
             transaction_type = 'CAPTURE')
            OR
            (capture_id IS NULL AND refund_id IS NOT NULL AND 
            transaction_type = 'REFUND')
        )
);

-- GRANT SELECT ON journal_entries TO payments_app;

-- GRANT INSERT (
--         id, payment_id, refund_id, capture_id, transaction_type
--     ) ON journal_entries TO payments_app;

CREATE TABLE journal_lines (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL,
    ledger_account_id UUID NOT NULL,
    direction VARCHAR(16) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_journal_lines_journal_entry
        FOREIGN KEY (journal_entry_id) REFERENCES journal_entries (id),
    CONSTRAINT fk_journal_lines_ledger_account
        FOREIGN KEY (ledger_account_id) REFERENCES ledger_accounts (id),
    CONSTRAINT chk_journal_lines_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_journal_lines_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_no_micro_cents_amount
        CHECK (amount * 100 = TRUNC(amount * 100)),
    CONSTRAINT chk_valid_currency_code CHECK (currency IN ('USD', 'CAD'))
);

-- GRANT SELECT ON journal_lines TO payments_app;

-- GRANT INSERT (
--         id, journal_entry_id, ledger_account_id, direction, amount, currency
--     ) ON journal_lines TO payments_app;

-- Ensure that the ledger is immutable
CREATE OR REPLACE FUNCTION block_ledger_updates_and_deletes()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Ledger records are immutable. Deletions and updates are '
                    'strictly prohibited.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_journal_entries
BEFORE UPDATE OR DELETE ON journal_entries
FOR EACH ROW EXECUTE FUNCTION block_ledger_updates_and_deletes();

CREATE TRIGGER trg_protect_journal_lines
BEFORE UPDATE OR DELETE ON journal_lines
FOR EACH ROW EXECUTE FUNCTION block_ledger_updates_and_deletes();

-- Ensure that every journal entry has at least two lines and is balanced by 
-- currency
CREATE OR REPLACE FUNCTION validate_journal_is_balanced()
RETURNS TRIGGER AS $$
DECLARE
    v_unbalanced_currency CHAR(3);
    v_excess_amount NUMERIC;
BEGIN
    -- Ensure at least two lines exist
    IF (SELECT count(*) FROM journal_lines WHERE journal_entry_id = NEW.id) < 2 
    THEN
        RAISE EXCEPTION 'Journal entry % has less than two journal lines.', 
        NEW.id;
    END IF;
    
    -- Check if any currency group for this journal entry does not sum to zero
    SELECT currency, SUM(
        CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END
        ) 
    INTO v_unbalanced_currency, v_excess_amount
    FROM journal_lines
    WHERE journal_entry_id = NEW.id
    GROUP BY currency
    HAVING SUM(CASE WHEN direction = 'DEBIT' THEN amount ELSE -amount END) != 0
    LIMIT 1;

    -- If we found an unbalanced currency, raise an error
    IF v_unbalanced_currency IS NOT NULL THEN
        RAISE EXCEPTION 'Journal entry % is unbalanced for currency %. Balance '
        'should be zero, but is % instead.', 
            NEW.id, v_unbalanced_currency, v_excess_amount;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trigger_check_journal_balance
AFTER INSERT ON journal_entries
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION validate_journal_is_balanced();

-- XOR check on journal_entries ensures that only one of capture_id or 
-- refund_id is not null
CREATE UNIQUE INDEX uk_journal_entries_capture_id
    ON journal_entries (capture_id)
    WHERE capture_id IS NOT NULL;

CREATE UNIQUE INDEX uk_journal_entries_refund_id
    ON journal_entries (refund_id)
    WHERE refund_id IS NOT NULL;

CREATE INDEX idx_journal_entries_payment_id
    ON journal_entries (payment_id);

CREATE INDEX idx_journal_lines_journal_entry_id
    ON journal_lines (journal_entry_id); 