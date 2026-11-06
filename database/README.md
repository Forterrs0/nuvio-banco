# Banco de dados MySQL

- **00_CRIAR_BANCO_MYSQL.sql** — execute no MySQL Workbench **antes** da primeira execução do Java. Troque a senha de exemplo **antes** de executar.
- **01_CONSULTAS_WORKBENCH.sql** — depois de iniciar o Nuvia, permite ver contas, saldos, transferências e histórico de migrações.
- **../src/main/resources/db/migration/V1__estrutura_inicial.sql** — estrutura completa das tabelas; o **Flyway executa automaticamente** ao iniciar o sistema. **Não execute o V1 manualmente no Workbench**, para não criar divergência no histórico de migrações.

O MySQL guarda os dados no armazenamento do próprio MySQL Server. Ao contrário do H2, **não existe `data/nuvia.mv.db`** nesta versão. Use o MySQL Workbench para visualizar as tabelas.

O banco `nuvia_db` começa vazio. Não há importação automática dos usuários/dados da versão H2 anterior.
