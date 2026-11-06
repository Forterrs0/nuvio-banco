# Nuvia — Banco acadêmico com MySQL (Java + Spring Boot)

O **Nuvia** é um protótipo bancário acadêmico com front-end responsivo, tema escuro/claro, teclado monetário animado e transferências persistentes **entre contas Nuvia cadastradas**.

> **Importante:** NÃO é um banco oficial. Não movimenta reais nem envia Pix pela rede bancária. Depósitos e "Pix" são **operações com saldo virtual demonstrativo**, registrados de verdade no banco MySQL.

## 1. Requisitos

- **Java JDK 21** (`java -version`)
- **Maven 3.9** (`mvn -version`)
- **MySQL Server 8.0 ou 8.4** (o serviço precisa estar iniciado)
- **MySQL Workbench** (recomendado para configurar e visualizar os dados)

> O Workbench sozinho não é o servidor. Instale e ligue o **MySQL Server** também. Na primeira execução o Maven precisará de internet para baixar as dependências.

## 2. Criar o banco MySQL — apenas uma vez

1. Abra o **MySQL Workbench** e conecte ao seu servidor local usando um usuário administrador, geralmente `root`.
2. Abra `database/00_CRIAR_BANCO_MYSQL.sql`.
3. **Substitua `TROQUE_ESTA_SENHA_ANTES_DE_EXECUTAR` por uma senha forte, nas duas ocorrências.**
4. Execute o script usando o botão de raio do Workbench.
5. O script criará o banco `nuvia_db` e o usuário local `nuvia_app`. Guarde a senha que você configurou.

O script de criação **não cria as tabelas**; isso é responsabilidade do **Flyway** quando o Java iniciar. O script da estrutura está em `src/main/resources/db/migration/V1__estrutura_inicial.sql` e é versionado.

## 3. Iniciar o site no Windows

1. Na pasta do projeto, dê dois cliques em **`INICIAR_NUVIA.bat`**.
2. Quando solicitado, informe a **senha MySQL do usuário `nuvia_app`** que você escolheu no passo 2. A senha não aparecerá enquanto você digita.
3. Aguarde a mensagem de inicialização do Spring Boot.
4. Abra **http://localhost:8080** no navegador.
5. Encerre com `Ctrl+C`.

O iniciador usa PowerShell para solicitar a senha sem mostrar os caracteres no terminal e não a grava em arquivo. Também é possível usar um terminal PowerShell diretamente, com leitura de senha oculta:

```powershell
.\INICIAR_NUVIA.ps1
```

O script solicitará a senha e iniciará o Maven automaticamente.

## 4. Ver o banco no Workbench

1. Conecte no servidor MySQL.
2. No painel **SCHEMAS**, atualize (refresh) e abra **`nuvia_db`**.
3. Após o primeiro início do Java, estarão lá: **`user_accounts`**, **`ledger_entries`** e **`flyway_schema_history`**.
4. Para ver os dados, abra e execute `database/01_CONSULTAS_WORKBENCH.sql`.

| Tabela | Para que serve |
|---|---|
| `user_accounts` | Nome, e-mail, telefone, hash da senha, saldo em centavos |
| `ledger_entries` | Depósitos virtuais e transferências entre contas, com data e identificadores |
| `flyway_schema_history` | Histórico das migrações SQL aplicadas pelo Flyway |

O MySQL guarda os dados em seu próprio diretório de armazenamento, que depende da instalação do MySQL Server. **Não espere um arquivo `nuvia.mv.db`: o H2 foi removido da execução principal.**

## 5. Teste uma transferência completa

1. Entre em `http://localhost:8080` e registre a conta A, com e-mail e telefone únicos.
2. Clique em **Depositar** e digite `5`, `0`, `0` → **R$ 5,00**. Confirme: a conta A recebe saldo **virtual** no MySQL.
3. Abra uma guia anônima ou outro perfil do navegador e registre a conta B, com outro e-mail e telefone.
4. Volte para A: selecione **Fazer Pix**, digite o **e-mail ou telefone da conta B**, e confirme o valor e o destinatário.
5. O saldo da conta A diminui e o da conta B aumenta, na **mesma transação de banco**. As duas contas possuem extratos e comprovantes da operação.
6. Confira os resultados pelo script `database/01_CONSULTAS_WORKBENCH.sql`.

O campo monetário funciona assim: `5` = R$ 0,05; `50` = R$ 0,50; `500` = R$ 5,00; `5000` = R$ 50,00. A digitação tem uma animação leve. O gráfico mostra entradas, saídas e saldo líquido por mês ao passar o mouse ou focar/tocar nas colunas.

## 6. Como o MySQL foi integrado

- Spring Data JPA / Hibernate utiliza o **driver `mysql-connector-j`**.
- `application.yml` aponta para `jdbc:mysql://localhost:3306/nuvia_db` (host, porta e banco configuráveis).
- **Flyway** executa os SQL versionados ao iniciar o servidor, criando as tabelas na primeira execução.
- `spring.jpa.hibernate.ddl-auto=validate`: o Java verifica que a estrutura corresponde às entidades, sem alterar silenciosamente o esquema.
- As tabelas usam **InnoDB** e chaves estrangeiras, exclusividade de e-mail/telefone, saldo guardado como inteiro de centavos, e uma restrição de idempotência para não duplicar operações.
- Cada transferência executa **débito + crédito + histórico em uma transação ACID**, com bloqueio pessimista dos usuários envolvidos e checagem de saldo.
- Consultas e movimentações são realizadas no backend; o navegador não decide qual saldo será debitado.

Variáveis opcionais de conexão:

```text
NUVIA_DB_HOST=localhost
NUVIA_DB_PORT=3306
NUVIA_DB_NAME=nuvia_db
NUVIA_DB_USER=nuvia_app
NUVIA_DB_PASSWORD=(senha definida no setup; obrigatoria)
NUVIA_DB_SSL_MODE=PREFERRED
```

**Segurança:** a configuração local é adequada para laboratório, não para disponibilizar na internet. Em deploy futuro, usar TLS verificado na conexão MySQL (`VERIFY_IDENTITY`), cookie `Secure`, hospedagem e credenciais seguras, rate limiting e backups. Não configure `allowPublicKeyRetrieval=true` em redes não confiáveis.

## 7. Testes

Rode `TESTAR_NUVIA.bat` ou:

```powershell
mvn test
```

Os **testes automatizados rápidos** usam um H2 **temporário em memória**, apenas no escopo de testes (`src/test/resources/application-test.yml`). A aplicação normal e os dados dos usuários usam **somente MySQL**. Para validar o MySQL de ponta a ponta, execute os passos da seção 5 com o servidor MySQL ligado.

Para gerar um JAR (ainda sem deploy):

```powershell
mvn clean package
```

**Limitação de validação desta entrega:** o MySQL Server e o Maven não estavam disponíveis no ambiente de empacotamento. O projeto e os scripts foram revisados estaticamente, mas você precisará executar `mvn test` e o teste prático da seção 5 no seu computador.

## 8. Organização dos arquivos

```text
Nuvia-Banco-MySQL/
├── INICIAR_NUVIA.bat
├── INICIAR_NUVIA.ps1
├── TESTAR_NUVIA.bat
├── pom.xml
├── README.md
├── database/
│   ├── 00_CRIAR_BANCO_MYSQL.sql
│   ├── 01_CONSULTAS_WORKBENCH.sql
│   └── README.md
└── src/
    ├── main/java/br/com/nuvia/      # backend Java
    ├── main/resources/application.yml
    ├── main/resources/db/migration/V1__estrutura_inicial.sql
    ├── main/resources/static/          # frontend existente
    └── test/                           # testes isolados
```

**Nenhum deploy foi realizado.** Vamos deixar isso para quando você pedir.
