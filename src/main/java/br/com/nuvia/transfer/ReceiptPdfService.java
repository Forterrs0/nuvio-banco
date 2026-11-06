package br.com.nuvia.transfer;

import br.com.nuvia.api.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Comprovante PDF de operação do ambiente acadêmico, não equivalente a comprovante bancário real. */
@Service
public class ReceiptPdfService {
    private static final float WIDTH = PDRectangle.A4.getWidth();
    private static final float HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 55;
    private static final float RIGHT = WIDTH - LEFT;
    private static final Locale LOCALE = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", LOCALE)
            .withZone(ZoneId.of("America/Bahia"));
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] generate(BankingService.EntryView tx) {
        boolean deposit = "DEMO_DEPOSIT".equals(tx.type());
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            document.getDocumentInformation().setTitle("Nuvia - comprovante demonstrativo");
            document.getDocumentInformation().setCreator("Nuvia Banco - projeto academico");
            try (PDPageContentStream canvas = new PDPageContentStream(document, page)) {
                fill(canvas, 0.045f, 0.09f, 0.13f);
                canvas.addRect(0, HEIGHT - 195, WIDTH, 195);
                canvas.fill();
                fill(canvas, 0.44f, 0.92f, 0.79f);
                canvas.addRect(LEFT, HEIGHT - 72, 9, 25);
                canvas.fill();
                text(canvas, BOLD, 26, LEFT + 19, HEIGHT - 68, "nuvia.", 1f, 1f, 1f);
                text(canvas, BOLD, 10, LEFT, HEIGHT - 112, "COMPROVANTE DE OPERACAO", .44f, .92f, .79f);
                text(canvas, BOLD, 28, LEFT, HEIGHT - 150, brl(Math.abs(tx.amountCents())), 1f, 1f, 1f);
                text(canvas, REGULAR, 11, LEFT, HEIGHT - 172,
                        deposit ? "Deposito demonstrativo concluido" : "Transferencia interna concluida", .85f, .9f, .94f);

                fill(canvas, .91f, .96f, .95f);
                canvas.addRect(LEFT, HEIGHT - 235, RIGHT - LEFT, 24);
                canvas.fill();
                text(canvas, BOLD, 9, LEFT + 12, HEIGHT - 226,
                        "PROJETO ACADEMICO  -  OPERACAO SIMULADA", .08f, .34f, .29f);

                float y = HEIGHT - 278;
                y = row(canvas, "DATA E HORA", DATETIME.format(tx.createdAt()), y);
                y = row(canvas, "TIPO", deposit ? "Deposito de saldo virtual" : "Transferencia entre contas Nuvia", y);
                y = row(canvas, "REMETENTE / ORIGEM", tx.senderName(), y);
                y = row(canvas, "DESTINATARIO", tx.recipientName(), y);
                y = row(canvas, "DESCRICAO", tx.description(), y);
                y = row(canvas, "SITUACAO", "Concluida", y);
                y = row(canvas, "IDENTIFICADOR UNICO", tx.id(), y);

                stroke(canvas, .78f, .84f, .85f);
                canvas.setLineWidth(.6f);
                canvas.moveTo(LEFT, 118);
                canvas.lineTo(RIGHT, 118);
                canvas.stroke();
                text(canvas, BOLD, 11, LEFT, 95, "NUVIA / DEMO", .11f, .22f, .26f);
                text(canvas, REGULAR, 9, LEFT, 75, "Registro interno do sistema de demonstracao Nuvia.", .35f, .42f, .46f);
                text(canvas, REGULAR, 9, LEFT, 59, "Nao e um comprovante Pix oficial nem confirma movimentacao bancaria real.", .35f, .42f, .46f);
            }
            document.save(buffer);
            return buffer.toByteArray();
        } catch (IOException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o PDF do comprovante.");
        }
    }

    private static float row(PDPageContentStream canvas, String label, String value, float y) throws IOException {
        text(canvas, BOLD, 9, LEFT, y, label, .36f, .43f, .47f);
        float current = y - 19;
        for (String line : wrap(value, REGULAR, 10.7f, RIGHT - LEFT)) {
            text(canvas, REGULAR, 10.7f, LEFT, current, line, .07f, .14f, .18f);
            current -= 13;
        }
        stroke(canvas, .86f, .9f, .9f);
        canvas.setLineWidth(.6f);
        canvas.moveTo(LEFT, current - 5);
        canvas.lineTo(RIGHT, current - 5);
        canvas.stroke();
        return current - 21;
    }

    private static java.util.List<String> wrap(String value, PDType1Font font, float fontSize, float maxWidth)
            throws IOException {
        String content = safe(value);
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : content.split("\\s+")) {
            if (word.isEmpty()) continue;
            String test = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.getStringWidth(test) * fontSize / 1000 > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            // Quebra identificadores ou termos muito grandes sem extrapolar a pagina.
            for (int index = 0; index < word.length(); index++) {
                String candidate = line.toString() + word.charAt(index);
                if (font.getStringWidth(candidate) * fontSize / 1000 > maxWidth && !line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(word.charAt(index));
            }
            line.append(' ');
        }
        if (!line.isEmpty()) lines.add(line.toString().trim());
        if (lines.isEmpty()) lines.add("-");
        return lines;
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        StringBuilder result = new StringBuilder();
        CharsetEncoder encoder = Charset.forName("windows-1252").newEncoder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Latin-1/WinAnsi suportado pela fonte padrao PDF, remove controles e caracteres nao codificaveis.
            if (c == '\n' || c == '\r' || c == '\t') c = ' ';
            if (Character.isISOControl(c) || !encoder.canEncode(c)) c = '?';
            result.append(c);
        }
        return result.toString().strip();
    }

    private static String brl(long cents) {
        return java.text.NumberFormat.getCurrencyInstance(LOCALE).format(cents / 100.0);
    }
    private static void text(PDPageContentStream canvas, PDType1Font font, float size, float x, float y,
                             String value, float r, float g, float b) throws IOException {
        fill(canvas, r, g, b);
        canvas.beginText();
        canvas.setFont(font, size);
        canvas.newLineAtOffset(x, y);
        canvas.showText(safe(value));
        canvas.endText();
    }
    private static void fill(PDPageContentStream canvas, float r, float g, float b) throws IOException {
        canvas.setNonStrokingColor(r, g, b);
    }
    private static void stroke(PDPageContentStream canvas, float r, float g, float b) throws IOException {
        canvas.setStrokingColor(r, g, b);
    }
}
