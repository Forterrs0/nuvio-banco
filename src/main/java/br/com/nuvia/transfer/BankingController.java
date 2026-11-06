package br.com.nuvia.transfer;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankingController {
    private final BankingService banking;
    public BankingController(BankingService banking) { this.banking = banking; }

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
