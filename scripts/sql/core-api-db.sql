CREATE TYPE account_type AS ENUM(
    'CUSTOMER',
    'RESERVED_FUNDS',
    'EXTERNAL',
    'FEES'
);

CREATE TYPE account_status AS ENUM(
    'ACTIVE',
    'BLOCKED',
    'CLOSED',
    'SUSPENDED'
);

CREATE TABLE IF NOT EXISTS accounts (
   id UUID PRIMARY KEY,
   iban VARCHAR(34) NOT NULL,
   bic VARCHAR(11) NOT NULL,
   type account_type NOT NULL DEFAULT 'CUSTOMER',
   current_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
   reserved_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
   currency VARCHAR(3) NOT NULL,
   status account_status NOT NULL DEFAULT 'ACTIVE',
   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
   updated_at TIMESTAMPTZ NULL,

   CONSTRAINT chk_current_balance_non_negative
        CHECK (current_balance >= 0),
   CONSTRAINT chk_reserved_balance_non_negative
        CHECK (reserved_balance >= 0),
   CONSTRAINT chk_reserved_balance_not_exceed_current
        CHECK (reserved_balance <= current_balance),
   CONSTRAINT chk_currency_length
        CHECK (char_length(currency) = 3),
   CONSTRAINT chk_iban_length
        CHECK (char_length(iban) BETWEEN 15 AND 34),
   CONSTRAINT chk_bic_length
        CHECK (char_length(bic) IN (8, 11))
);
