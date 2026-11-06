# Nuvia — painel administrativo de usuários

Esta atualização acrescenta um painel interno para **visualizar** usuários cadastrados no MySQL. Não altera saldo, movimentações, login de clientes nem contas já criadas. Não publica dados fora do servidor.

## Instalação

1. **Faça uma cópia de segurança** da pasta atual e do banco de dados `nuvia_db` antes de aplicar mudanças. Se você já alterou `app.js`, `styles.css` ou classes Java depois de gerar o projeto original, faça o merge das alterações, em vez de substituir seus arquivos sem conferência.
2. Copie os arquivos atualizados para as **mesmas pastas** do repositório Nuvia. Não substitua `V1__estrutura_inicial.sql` nem execute alterações manuais na tabela para criar a coluna.
3. Inicie normalmente com `INICIAR_NUVIA.bat` ou `mvn spring-boot:run`. O **Flyway aplicará automaticamente V2__perfil_administrador.sql**, adicionando `is_admin` com valor `FALSE` a todos os usuários atuais. As contas existentes são preservadas.
4. Se ainda não tiver uma conta pessoal, crie-a pelo site. No **MySQL Workbench**, conectado com permissões para atualizar a tabela, abra `database/02_ATIVAR_ADMIN.sql`, substitua `SEU_EMAIL_AQUI@EXEMPLO.COM` pelo seu e-mail cadastrado nas duas linhas, e execute. Confirme que **uma linha** foi atualizada e que `is_admin` ficou igual a `1`.
5. **Saia da conta e entre novamente** (as permissões são carregadas no login). Clique em **Usuários** no menu lateral. O painel tem tema escuro/claro, total de contas, novos cadastros nos últimos 30 dias, administradores, pesquisa e paginação (10 por página).

## Proteções

- O navegador só mostra o link para contas com campo `admin=true`, mas a proteção efetiva fica no **Spring Security**: `GET /api/admin/users` e `GET /api/admin/summary` exigem `ROLE_ADMIN`.
- Cadastros comuns não podem escolher ou solicitar a permissão de administrador; a promoção é feita diretamente no MySQL com credenciais administrativas. Não adicione rotas públicas para conceder esse privilégio.
- As respostas enviam **ID, nome, e-mail, telefone, data de cadastro e tipo de perfil**; não retornam senha, hash da senha, saldos ou extratos. Páginas têm até 50 usuários por requisição e não devem ser publicadas sem autenticação.
- **Não use o usuário de banco da aplicação como administrador do site.** São coisas separadas: a permissão `is_admin` da conta de pessoa controla a interface, e o usuário MySQL é apenas a credencial de conexão do servidor.

## Endpoints

- `GET /api/admin/summary`: `totalUsers`, `administrators`, `newLast30Days`.
- `GET /api/admin/users?page=0&size=10&search=ana`: `users`, `total`, `totalPages`, `page`, `size` (ordenados pelos cadastros mais recentes).

## Conferência e testes

Execute `mvn test`, que inclui `AdminAccessTest` para verificar que clientes recebem **403** nas rotas administrativas, que visitantes recebem **401**, que o administrador recebe **200** e que o cadastro nasce sem permissão administrativa.

Se a aba **Usuários** não aparecer: confirme que a tabela `user_accounts` contém `is_admin=1` para seu e-mail, confira que o Flyway executou V2 e faça logout/login. Se a API responder 403, o servidor não reconhece sua sessão como administradora.

> Este é um projeto acadêmico. Para hospedar na internet, proteja HTTPS, configurações de sessão, informações pessoais e backups e revise permissões conforme requisitos de proteção de dados.
