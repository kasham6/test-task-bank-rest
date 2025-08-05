CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       username VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(100) NOT NULL DEFAULT 'USER',
                       enabled BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cards (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       number_encrypted TEXT NOT NULL,
                       owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       expiry DATE NOT NULL,
                       status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
                       balance NUMERIC(19,4) NOT NULL DEFAULT 0.00,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE transfers (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           from_card_id UUID REFERENCES cards(id) ON DELETE SET NULL,
                           to_card_id UUID REFERENCES cards(id) ON DELETE SET NULL,
                           user_id UUID NOT NULL REFERENCES users(id),
                           amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
                           status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION mask_card_number(plain TEXT)
    RETURNS TEXT AS $$
DECLARE
    last4 TEXT;
BEGIN
    IF length(plain) < 4 THEN
        RETURN repeat('*', length(plain));
    END IF;
    last4 := right(plain, 4);
    RETURN '**** **** **** ' || last4;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_cards_updated_at BEFORE UPDATE ON cards
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_transfers_updated_at BEFORE UPDATE ON transfers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX idx_cards_owner_id ON cards(owner_id);
CREATE INDEX idx_transfers_from_card_id ON transfers(from_card_id);
CREATE INDEX idx_transfers_to_card_id ON transfers(to_card_id);
CREATE INDEX idx_transfers_user_id ON transfers(user_id);

INSERT INTO users (id, username, password_hash, role, enabled)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'admin',
           '$2a$10$WaA6qxew1rzPhQqDZ75eL.K0bxMBXme1LVw3v9Ao.LdZEP76U2coO',
           'ADMIN',
           true
       ) ON CONFLICT (username) DO NOTHING;
