# Nuvia — Banco acadêmico (Java + Web)

Projeto completo, com front-end responsivo e back-end Spring Boot, criado para demonstração de operações financeiras internas entre contas **Nuvia**. Mantém uma identidade visual escura em verde-menta e também oferece modo claro.

> **AVISO IMPORTANTE:** Este projeto **não é um banco**, não se conecta à rede Pix oficial, não aceita pagamentos reais e não movimenta dinheiro de verdade. O depósito credita **saldo de demonstração**, persistido no banco de dados local. O "Pix" transfere esse saldo **somente entre contas cadastradas neste mesmo site**. Comprovantes são demonstrativos.

## Começar no Windows

**Requisitos:** Java JDK 21 e Apache Maven 3.9. O Maven 3.9 é suportado pelo Spring Boot 3.5.16.

1. Extraia esta pasta em um diretório como `C:\Projetos\Nuvia-Banco`.
2. Abra um terminal **na pasta em que está o `pom.xml`**.
3. Confirme `java -version` e `mvn -version`. Você também pode iniciar com **duplo clique em `INICIAR_NUVIA.bat`**.
4. Execute:

   ```powershell
   mvn spring-boot:run
   ```

5. Acesse no navegador: **http://localhost:8080**.
6. Para parar o servidor, pressione `Ctrl+C` no terminal.

Na primeira execução, o Maven precisa de **internet** para baixar as dependências. Depois, o H2 salva as contas e transações em `./data/nuvia.mv.db`. O banco de dados permanece ao reiniciar a aplicação (não delete a pasta `data`).

Se a porta 8080 estiver ocupada:

```powershell
$env:PORT=8081
mvn spring-boot:run
```

## Testando transferências entre usuários

1. Abra http://localhost:8080 em uma janela normal do navegador.
2. Clique em **Criar conta** e cadastre o usuário **Matheus**, com e-mail e telefone próprios.
3. Entre na conta e clique em **Depositar**. Digite `5`, `0`, `0`: aparece **R$ 5,00**. Confirme o depósito demonstrativo.
4. Abra outra janela anônima ou outro perfil de navegador. Cadastre **José**, com **outro e-mail e outro telefone**.
5. Volte à janela de Matheus e escolha **Fazer Pix**. Informe o **e-mail ou telefone** cadastrado para José, digite o valor, clique em **Continuar**, confira o destinatário e confirme.
6. Confira o novo saldo e o extrato nas **duas** janelas. Se José atualizar o painel, encontrará o Pix recebido.
7. Clique em uma movimentação para visualizar e copiar seu comprovante demonstrativo.

O gráfico exibe entradas, saídas, saldo líquido e quantidade de transações por mês ao passar o mouse (ou tocar/focar) nas colunas.

## Como a digitação funciona

O campo de valor recebe apenas dígitos e apresenta o número como centavos:

| Digitação | Exibição |
|---|---|
| `5` | R$ 0,05 |
| `50` | R$ 0,50 |
| `500` | R$ 5,00 |
| `5000` | R$ 50,00 |

Ao digitar, o número faz uma animação leve de entrada. Colar números, usar Backspace e teclado numérico também são suportados. O back-end valida **inteiros em centavos**; números informados pelo navegador não são usados como autorização de saldo.

## Funcionalidades implementadas

- Cadastro de usuários com nome, e-mail, telefone brasileiro com DDD e senha criptografada com BCrypt.
- Login com sessão HTTP e proteção CSRF, logout e rotas autenticadas.
- E-mail e telefone únicos, normalizados; busca de destinatário **somente entre usuários Nuvia**.
- Depósitos demonstrativos com valores persistidos em H2.
- Transferências internas entre usuários, por e-mail ou telefone, com débito e crédito numa única transação ACID.
- Bloqueio pessimista de contas em ordem de ID para reduzir conflitos de concorrência, mais chave idempotente para evitar duplicidade em tentativas repetidas.
- Validação de valor mínimo e limite de R$ 100.000,00 por operação, de saldo suficiente, de destinatário existente e de transferência para si mesmo.
- Histórico, comprovantes, filtros no extrato, gráfico por mês, visão mensal e atualização dos saldos após confirmação.
- Tema escuro/claro, layout responsivo, animações discretas e respeito a `prefers-reduced-motion`.
- Front-end servido **pelo próprio Spring Boot** — sem servidor Node.js ou build React separado.

## Organização

```text
Nuvia-Banco/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/br/com/nuvia/
│   │   │   ├── NuviaApplication.java
│   │   │   ├── config/SecurityConfig.java
│   │   │   ├── auth/                  # cadastro, login, sessão
│   │   │   ├── account/               # conta e normalização de telefone
│   │   │   ├── transfer/              # serviços transacionais e extrato
│   │   │   └── api/                   # tratamento de erros
│   │   └── resources/
│   │       ├── application.yml        # banco H2 persistente
│   │       └── static/                # HTML, CSS, JS e ícone
│   └── test/                           # testes Java
└── data/ (criada automaticamente, não incluída no ZIP)
```

## API (mesma origem, sem CORS aberto)

| Método | Caminho | Função |
|---|---|---|
| GET | `/api/auth/csrf` | Gera token CSRF da sessão |
| POST | `/api/auth/register` | Cadastra cliente |
| POST | `/api/auth/login` | Faz login (form-urlencoded) |
| POST | `/api/auth/logout` | Encerra a sessão |
| GET | `/api/me` | Dados da conta autenticada |
| GET | `/api/dashboard` | Saldo, movimentações e gráfico |
| GET | `/api/transactions?limit=100` | Extrato |
| GET | `/api/transactions/{id}` | Comprovante autorizado |
| GET | `/api/recipients/resolve?identifier=` | Identifica conta destinatária |
| POST | `/api/transactions/deposit` | Crédito virtual demonstrativo |
| POST | `/api/transactions/transfer` | Transferência interna entre clientes |

As solicitações de depósito e transferência recebem `amountCents` (inteiro) e `idempotencyKey` (UUID). A chave evita repetir a mesma transferência caso o usuário envie a mesma operação novamente. Uma chave reutilizada para valores ou destinatários diferentes retorna erro.

## Testes e pacote executável

```powershell
mvn test
mvn clean package
java -jar target/nuvia-banco-1.0.0.jar
```

**Nota:** na entrega, o código Java foi preparado e o JavaScript validado quanto à sintaxe, mas a compilação Maven e os testes de integração **não puderam ser executados neste ambiente**, pois não há Maven instalado nem acesso ao Maven Central. Execute `mvn test` no seu computador para comprovar a integração de dependências e identificar eventuais ajustes antes de apresentar.

## Antes de qualquer deploy (ainda NÃO realizado)

Esta versão é intencionalmente um **protótipo acadêmico local**. Para disponibilizar online, serão necessários, no mínimo: banco PostgreSQL com migrações de esquema, HTTPS e cookies Secure obrigatórios, variáveis de ambiente seguras, limite de tentativas de login e consulta de destinatários, observabilidade, políticas de recuperação/backup, auditoria, validação de posse de e-mail/telefone, limites de movimentação por usuário e testes automatizados completos de concorrência e segurança. Depósitos demonstrativos precisam continuar claramente identificados e isolados de qualquer integração bancária de verdade.

**Nenhum deploy, conta em nuvem, domínio ou infraestrutura foi configurado.**
