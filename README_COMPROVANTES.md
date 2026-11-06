# Nuvia — comprovantes para imprimir, baixar e compartilhar

Atualização sobre a versão **Nuvia com painel de usuários** (Java 21 + Spring Boot 3.5 + MySQL 8.x).

## Instalar sem apagar o banco ou o painel

1. **Faça backup** de sua pasta atual do Nuvia e pare o servidor no terminal (`Ctrl + C`).
2. Para atualizar a versão que já está funcionando, abra a pasta do projeto, onde está o `pom.xml`, e **extraia nela o conteúdo de `Nuvia-Atualizacao-Comprovantes.zip`**, preservando os caminhos `src/...` e substituindo apenas os arquivos indicados. Não substitua a pasta inteira pela pasta da atualização.
3. Os arquivos `pom.xml`, `BankingController.java`, `app.js` e `styles.css` foram alterados; `ReceiptPdfService.java` é novo, assim como os testes.
4. Inicie novamente pelo `INICIAR_NUVIA.bat`. O Maven precisará obter a biblioteca `org.apache.pdfbox:pdfbox:3.0.8` na primeira compilação (internet necessária se ela ainda não está no cache).
5. Abra `http://localhost:8080`, faça login e pressione `Ctrl + Shift + R` para recarregar os arquivos do navegador.
6. Faça uma operação demonstrativa ou abra **Extrato** > uma movimentação existente. O modal mostra os quatro botões abaixo.

## Botões

- **Imprimir**: abre a impressão do navegador com layout de comprovante adequado para papel A4. Também é possível escolher **Salvar como PDF** na janela de impressão.
- **Baixar PDF**: solicita ao servidor um PDF real da transação salva no MySQL, baixando `nuvia-comprovante-<id>.pdf`.
- **Compartilhar**: usa a função nativa de compartilhamento de arquivos do navegador, quando disponível. Se não estiver, baixa o PDF para anexar manualmente no WhatsApp, e-mail etc.
- **Copiar dados**: copia um resumo em texto da transação.

## Segurança e integridade

- O endpoint é `GET /api/transactions/{id}/receipt.pdf?download=true` (PDF como anexo) ou sem `download` (PDF para visualização no navegador).
- A mesma consulta do comprovante em `/api/transactions/{id}` valida a sessão e se o usuário pertence ao remetente ou destinatário. Um terceiro recebe HTTP 404. A rota não é pública.
- PDFs contêm somente dados existentes no histórico; não são aceitos valores ou nomes informados pelo frontend para a geração do documento.
- A resposta usa `Cache-Control: no-store`.
- **O Nuvia é um projeto acadêmico**, com saldo e transferências simuladas. O documento informa que não equivale a um comprovante Pix real.
- **Não há atualização no esquema MySQL**, nenhuma tabela é criada ou apagada, e o painel administrativo permanece inalterado.

## Testar

No terminal dentro da pasta do projeto:

```powershell
mvn test
```

Os novos testes incluem verificação do conteúdo do PDF e acesso restrito a participantes da transação. O frontend pode ser verificado com `node --check src/main/resources/static/app.js` se o Node estiver instalado.

**Atenção:** se você alterou manualmente `app.js` ou `styles.css` depois da versão do Nuvia entregue anteriormente, faça um backup e compare os arquivos antes de substituí-los, para não perder mudanças recentes.
