-- Flyway V1: esquema MySQL do Nuvia.
-- Executado automaticamente pelo Spring Boot na primeira inicializacao.
-- Valores monetarios sao armazenados em centavos (BIGINT), nunca FLOAT.

CREATE TABLE user_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(80) NOT NULL,
  email VARCHAR(180) NOT NULL,
  phone VARCHAR(18) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  balance_cents BIGINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_account_email UNIQUE (email),
  CONSTRAINT uk_account_phone UNIQUE (phone),
  CONSTRAINT ck_accounts_balance_nonnegative CHECK (balance_cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ledger_entries (
  id BIGINT NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(36) NOT NULL,
  type VARCHAR(24) NOT NULL,
  sender_id BIGINT NULL,
  recipient_id BIGINT NOT NULL,
  initiator_id BIGINT NOT NULL,
  idempotency_key VARCHAR(36) NOT NULL,
  amount_cents BIGINT NOT NULL,
  description VARCHAR(140) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_ledger_public_id UNIQUE (public_id),
  CONSTRAINT uk_ledger_idempotency UNIQUE (initiator_id, idempotency_key),
  CONSTRAINT fk_ledger_sender FOREIGN KEY (sender_id) REFERENCES user_accounts(id),
  CONSTRAINT fk_ledger_recipient FOREIGN KEY (recipient_id) REFERENCES user_accounts(id),
  CONSTRAINT fk_ledger_initiator FOREIGN KEY (initiator_id) REFERENCES user_accounts(id),
  CONSTRAINT ck_ledger_positive_amount CHECK (amount_cents > 0),
  INDEX idx_ledger_created (created_at),
  INDEX idx_ledger_sender (sender_id),
  INDEX idx_ledger_recipient (recipient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
