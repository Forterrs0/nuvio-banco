-- Execute no MySQL Workbench DEPOIS de iniciar o servidor Java uma vez.
-- O Flyway cria as tabelas automaticamente no primeiro inicio.
USE nuvia_db;
SHOW TABLES;

-- Usuarios cadastrados (sem expor hashes de senhas)
SELECT id, full_name, email, phone,
       ROUND(balance_cents / 100, 2) AS saldo_reais,
       created_at
FROM user_accounts ORDER BY id;

-- Depositos demonstrativos e transferencias internas
SELECT l.public_id, l.type, l.created_at,
       remetente.full_name AS remetente,
       destino.full_name AS destinatario,
       ROUND(l.amount_cents / 100, 2) AS valor_reais,
       l.description
FROM ledger_entries l
LEFT JOIN user_accounts remetente ON remetente.id = l.sender_id
JOIN user_accounts destino ON destino.id = l.recipient_id
ORDER BY l.created_at DESC, l.id DESC;

-- Historico de migracoes do schema
SELECT installed_rank, version, description, success
FROM flyway_schema_history ORDER BY installed_rank;
