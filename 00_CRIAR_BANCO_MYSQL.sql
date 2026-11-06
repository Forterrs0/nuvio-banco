-- Execute no MySQL Workbench como administrador (ex.: root).
-- Requer MySQL Server 8.0+; cria a base sem apagar bancos existentes.
CREATE DATABASE IF NOT EXISTS nuvia_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- Opcional: um usuario especifico para o Nuvia.
-- ANTES DE EXECUTAR, TROQUE A SENHA ABAIXO POR UMA SENHA FORTE.
-- Se ja criou esse usuario em outra tentativa, nao esqueça de ajustar a senha
-- nas variaveis de ambiente do sistema.
CREATE USER IF NOT EXISTS 'nuvia_app'@'localhost'
  IDENTIFIED BY 'TROQUE_ESTA_SENHA_ANTES_DE_EXECUTAR';

-- Garante que a senha escolhida fique atualizada em instalacoes repetidas.
ALTER USER 'nuvia_app'@'localhost'
  IDENTIFIED BY 'careca1521212';

-- Permissoes limitadas ao banco do prototipo local (incluem migracoes Flyway).
GRANT ALL PRIVILEGES ON nuvia_db.* TO 'nuvia_app'@'localhost';

-- Conferencia:
SHOW DATABASES LIKE 'nuvia_db';
