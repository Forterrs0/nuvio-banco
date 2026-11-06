package br.com.nuvia;

import br.com.nuvia.transfer.BankingService;
import br.com.nuvia.transfer.ReceiptPdfService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ReceiptPdfServiceTest {
    @Test
    void generatedPdfContainsRealTransactionDataAndDemoWarning() throws Exception {
        var entry = new BankingService.EntryView("00000000-0000-0000-0000-000000000001","INTERNAL_PIX","OUT",
                -12345L,"Joao Silva","Pagamento do almoco",Instant.parse("2026-11-06T12:00:00Z"),
                "Ana Lima","Joao Silva");
        byte[] bytes = new ReceiptPdfService().generate(entry);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(1,pdf.getNumberOfPages());
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("123,45"), text);
            assertTrue(text.contains("Ana Lima"), text);
            assertTrue(text.contains("Joao Silva"), text);
            assertTrue(text.contains(entry.id()), text);
            assertTrue(text.contains("SIMULADA"), text);
        }
    }
}
