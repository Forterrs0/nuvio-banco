
/* Nuvia — Exportação de comprovantes demonstrativos */
(function () {
  'use strict';

  const W = 900;
  const PAD = 76;

  const currency = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  });

  const money = cents =>
    currency.format(Math.abs(Number(cents) || 0) / 100);

  const date = value =>
    new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'long',
      timeStyle: 'short',
      timeZone: 'America/Bahia'
    }).format(new Date(value));

  const filename = tx =>
    'Nuvia-comprovante-' +
    String(tx.id ?? 'operacao')
      .replace(/[^a-z0-9_-]/gi, '')
      .slice(0, 30) + '.png';

  function wrapText(ctx, text, maxWidth) {
    const result = [];
    let current = '';

    for (const char of String(text || '—').replace(/\s+/g, ' ')) {
      if (ctx.measureText(current + char).width > maxWidth && current) {
        result.push(current.trim());
        current = char;
      } else {
        current += char;
      }
    }

    if (current.trim()) result.push(current.trim());

    return result.length ? result : ['—'];
  }

  function createReceipt(tx) {
    if (!tx || !tx.id) {
      throw new Error('Nenhuma transação disponível para o comprovante.');
    }

    const isDeposit = tx.type === 'DEMO_DEPOSIT';
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    if (!ctx) {
      throw new Error('Seu navegador não suporta gerar esta imagem.');
    }

    ctx.font = '600 26px Arial';

    const fields = [
      ['DATA E HORA', date(tx.createdAt)],
      ['REMETENTE', tx.senderName || 'Não informado'],
      ['DESTINATÁRIO', tx.recipientName || 'Não informado'],
      ['DESCRIÇÃO', tx.description || 'Não informada'],
      ['IDENTIFICADOR DA OPERAÇÃO', tx.id]
    ];

    const rows = fields.map(([label, value]) => ({
      label,
      lines: wrapText(ctx, value, W - PAD * 2)
    }));

    const rowHeight = row => 98 + row.lines.length * 38;
    const height =
      400 + rows.reduce((sum, row) => sum + rowHeight(row), 0) + 175;

    canvas.width = W;
    canvas.height = height;

    // Fundo do comprovante
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, W, height);

    // Cabeçalho Nuvia
    ctx.fillStyle = '#102d32';
    ctx.fillRect(0, 0, W, 330);

    ctx.fillStyle = '#7ff2d6';
    ctx.fillRect(0, 318, W, 12);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 55px Arial';
    ctx.fillText('nuvia.', PAD, 100);

    ctx.fillStyle = '#7ff2d6';
    ctx.font = 'bold 16px Arial';
    ctx.fillText('PROJETO ACADÊMICO', PAD, 153);

    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 60px Arial';
    ctx.fillText(money(tx.amountCents), PAD, 245);

    ctx.font = 'bold 18px Arial';
    ctx.fillStyle = '#b7dfd4';
    ctx.fillText(
      isDeposit ? 'DEPÓSITO DEMONSTRATIVO' : 'TRANSFERÊNCIA INTERNA',
      PAD,
      285
    );

    // Dados do comprovante
    let y = 400;

    for (const row of rows) {
      ctx.fillStyle = '#66827e';
      ctx.font = 'bold 17px Arial';
      ctx.fillText(row.label, PAD, y);

      y += 44;

      ctx.fillStyle = '#142f34';
      ctx.font = '600 26px Arial';

      for (const line of row.lines) {
        ctx.fillText(line, PAD, y);
        y += 38;
      }

      y += 20;

      ctx.strokeStyle = '#e1eae7';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(PAD, y);
      ctx.lineTo(W - PAD, y);
      ctx.stroke();

      y += 34;
    }

    // Aviso de demonstração
    const footerY = height - 154;

    ctx.fillStyle = '#edf8f3';
    ctx.fillRect(0, footerY, W, 154);

    ctx.fillStyle = '#17664c';
    ctx.font = 'bold 19px Arial';
    ctx.fillText(
      'COMPROVANTE DEMONSTRATIVO',
      PAD,
      footerY + 53
    );

    ctx.fillStyle = '#55716b';
    ctx.font = '17px Arial';
    ctx.fillText(
      'Operação simulada entre contas Nuvia.',
      PAD,
      footerY + 92
    );
    ctx.fillText(
      'Não comprova pagamentos ou transferências Pix reais.',
      PAD,
      footerY + 119
    );

    return canvas;
  }

  function download(tx) {
    const image = createReceipt(tx).toDataURL('image/png');
    const link = document.createElement('a');

    link.href = image;
    link.download = filename(tx);

    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  function print(tx) {
    // Abrir durante o clique evita bloqueio de pop-up.
    const popup = window.open('', '_blank');

    if (!popup) {
      throw new Error(
        'O navegador bloqueou a impressão. Permita pop-ups para o Nuvia.'
      );
    }

    popup.document.open();
    popup.document.write(`
      <!doctype html>
      <html lang="pt-BR">
      <head>
        <meta charset="UTF-8">
        <title>Comprovante demonstrativo Nuvia</title>
        <style>
          @page { size: A4; margin: 10mm; }
          body {
            margin: 0;
            background: white;
            display: flex;
            justify-content: center;
          }
          img {
            display: block;
            max-width: 100%;
            max-height: 277mm;
            width: auto;
            height: auto;
            object-fit: contain;
          }
          @media print {
            body { display: block; }
            img { margin: 0 auto; }
          }
        </style>
      </head>
      <body></body>
      </html>
    `);
    popup.document.close();

    const img = popup.document.createElement('img');

    img.onload = function () {
      popup.focus();
      popup.print();
    };

    popup.document.body.appendChild(img);
    img.src = createReceipt(tx).toDataURL('image/png');
  }

  async function share(tx) {
    const image = createReceipt(tx).toDataURL('image/png');
    const encoded = image.split(',')[1];

    const bytes = Uint8Array.from(
      atob(encoded),
      character => character.charCodeAt(0)
    );

    const file = new File([bytes], filename(tx), {
      type: 'image/png'
    });

    if (
      typeof navigator.share === 'function' &&
      typeof navigator.canShare === 'function' &&
      navigator.canShare({ files: [file] })
    ) {
      try {
        await navigator.share({
          title: 'Comprovante demonstrativo Nuvia',
          files: [file]
        });
        return 'shared';
      } catch (error) {
        if (error.name === 'AbortError') return 'cancelled';
        throw error;
      }
    }

    // Em navegadores sem compartilhamento de arquivos,
    // baixa o PNG para envio manual.
    download(tx);
    return 'downloaded';
  }

  window.NuviaReceipt = {
    download,
    print,
    share
  };
})();
