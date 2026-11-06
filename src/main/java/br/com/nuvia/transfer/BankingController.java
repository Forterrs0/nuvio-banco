package br.com.nuvia.transfer;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankingController {
    private final BankingService banking;
    private final ReceiptPdfService pdf;
    public BankingController(BankingService banking, ReceiptPdfService pdf) {
        this.banking = banking;
        this.pdf = pdf;
    }

    @GetMapping("/dashboard")
    BankingService.DashboardView dashboard(Authentication auth) {
        return banking.dashboard(auth.getName());
    }
    @GetMapping("/transactions")
    List<BankingService.EntryView> statement(Authentication auth, @RequestParam(defaultValue = "50") int limit) {
        return banking.statement(auth.getName(), limit);
    }
    @GetMapping("/transactions/{id}")
    BankingService.EntryView receipt(Authentication auth, @PathVariable String id) {
        return banking.receipt(auth.getName(), id);
    }
    /** O identificador pertence a uma transacao persistida; so participantes podem acessar. */
    @GetMapping(value = "/transactions/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> receiptPdf(Authentication auth, @PathVariable String id,
                                       @RequestParam(defaultValue = "false") boolean download) {
        BankingService.EntryView entry = banking.receipt(auth.getName(), id);
        byte[] bytes = pdf.generate(entry);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("nuvia-comprovante-" + entry.id() + ".pdf").build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }

    @GetMapping("/recipients/resolve")
    BankingService.RecipientView resolve(Authentication auth, @RequestParam String identifier) {
        return banking.resolveRecipient(auth.getName(), identifier);
    }
    @PostMapping("/transactions/deposit")
    BankingService.EntryView deposit(Authentication auth, @Valid @RequestBody BankingService.DepositRequest body) {
        return banking.depositDemo(auth.getName(), body);
    }
    @PostMapping("/transactions/transfer")
    BankingService.EntryView transfer(Authentication auth, @Valid @RequestBody BankingService.TransferRequest body) {
        return banking.transfer(auth.getName(), body);
    }
}
