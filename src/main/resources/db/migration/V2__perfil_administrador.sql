-- Todos os usuarios atuais e novos comecam sem privilegios administrativos.
-- A promocao de um administrador e manual, com autenticacao no Workbench.
ALTER TABLE user_accounts
  ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;
