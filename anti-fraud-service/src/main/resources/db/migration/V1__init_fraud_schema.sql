CREATE TABLE blacklisted_accounts (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fraud_check_audits (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    sender_account_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    approved BOOLEAN NOT NULL,
    reason VARCHAR(255),
    checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_audits_payment_id ON fraud_check_audits(payment_id);