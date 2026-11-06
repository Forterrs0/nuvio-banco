package br.com.nuvia.transfer;

import br.com.nuvia.account.AccountRepository;
import br.com.nuvia.account.PhoneNumber;
import br.com.nuvia.account.UserAccount;
import br.com.nuvia.api.ApiException;
import br.com.nuvia.auth.AuthService;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BankingService {
    public static final long MAX_OPERATION_CENTS = 10_000_000L; // R$ 100.000,00 (ambiente de demonstracao)
    private static final ZoneId BRAZIL = ZoneId.of("America/Bahia");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final AccountRepository accounts;
    private final LedgerRepository ledger;
    private final AuthService auth;
    public BankingService(AccountRepository accounts, LedgerRepository ledger, AuthService auth) {
        this.accounts = accounts;
        this.ledger = ledger;
        this.auth = auth;
    }

    public record DepositRequest(
        @NotNull(message = "Informe um valor.") @Min(value = 1, message = "O valor mínimo é R$ 0,01.")
        @Max(value = MAX_OPERATION_CENTS, message = "O limite por operação é R$ 100.000,00.") Long amountCents,
        @NotBlank(message = "Operação sem identificador.") String idempotencyKey
    ) {}
    public record TransferRequest(
        @NotBlank(message = "Informe o e-mail ou telefone do destinatário.") String identifier,
        @NotNull(message = "Informe um valor.") @Min(value = 1, message = "O valor mínimo é R$ 0,01.")
        @Max(value = MAX_OPERATION_CENTS, message = "O limite por operação é R$ 100.000,00.") Long amountCents,
        @Size(max = 140, message = "Descrição deve ter no máximo 140 caracteres.") String description,
        @NotBlank(message = "Operação sem identificador.") String idempotencyKey
    ) {}
    public record RecipientView(String name, String hint) {}
    public record EntryView(String id, String type, String direction, long amountCents,
                            String counterparty, String description, Instant createdAt,
                            String senderName, String recipientName) {}
    public record MonthView(String key, String label, long incomeCents, long expenseCents, int count) {}
    public record DashboardView(AuthService.UserView user, long incomeMonthCents,
                                long expenseMonthCents, List<MonthView> months, List<EntryView> recent) {}

    @Transactional(readOnly = true)
    public RecipientView resolveRecipient(String senderEmail, String identifier) {
        UserAccount sender = auth.requireUser(senderEmail);
        UserAccount recipient = findRecipient(identifier);
        if (recipient.getId().equals(sender.getId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Você não pode transferir para sua própria conta.");
        String hint = recipient.getEmail().replaceAll("(^.{1,2}).*(@.*$)", "$1***$2");
        return new RecipientView(recipient.getFullName(), hint);
    }

    @Transactional
    public EntryView depositDemo(String senderEmail, DepositRequest request) {
        long amount = checkAmount(request.amountCents());
        String key = checkKey(request.idempotencyKey());
        UserAccount account = lock(requireUserId(senderEmail));
        Optional<LedgerEntry> existing = ledger.findByInitiatorIdAndIdempotencyKey(account.getId(), key);
        if (existing.isPresent()) return replayDeposit(existing.get(), amount, account.getId());
        account.credit(amount);
        LedgerEntry created = ledger.save(new LedgerEntry(LedgerEntry.Type.DEMO_DEPOSIT, null,
                account, amount, "Depósito demonstrativo", key, account.getId()));
        return toView(created, account.getId());
    }

    @Transactional
    public EntryView transfer(String senderEmail, TransferRequest request) {
        long amount = checkAmount(request.amountCents());
        String key = checkKey(request.idempotencyKey());
        // Busque apenas IDs antes dos locks: carregar as entidades previamente poderia
        // manter saldo antigo no cache JPA durante transferencias simultaneas.
        Long originId = requireUserId(senderEmail);
        Long targetId = findRecipientId(request.identifier());
        if (originId.equals(targetId))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Você não pode transferir para sua própria conta.");

        // Adquire locks sempre em ordem crescente, evitando deadlocks entre transferencias cruzadas.
        long first = Math.min(originId, targetId);
        long second = Math.max(originId, targetId);
        UserAccount lockedFirst = lock(first);
        UserAccount lockedSecond = lock(second);
        UserAccount sender = originId.equals(first) ? lockedFirst : lockedSecond;
        UserAccount recipient = targetId.equals(first) ? lockedFirst : lockedSecond;

        Optional<LedgerEntry> existing = ledger.findByInitiatorIdAndIdempotencyKey(sender.getId(), key);
        if (existing.isPresent()) return replayTransfer(existing.get(), amount, sender.getId(), recipient.getId());
        if (sender.getBalanceCents() < amount)
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente para essa transferência.");

        sender.debit(amount);
        recipient.credit(amount);
        String description = request.description() == null || request.description().isBlank()
                ? "Transferência Nuvia" : request.description().trim();
        LedgerEntry created = ledger.save(new LedgerEntry(LedgerEntry.Type.INTERNAL_PIX,
                sender, recipient, amount, description, key, sender.getId()));
        return toView(created, sender.getId());
    }

    @Transactional(readOnly = true)
    public EntryView receipt(String senderEmail, String publicId) {
        UserAccount user = auth.requireUser(senderEmail);
        LedgerEntry entry = ledger.findByPublicId(publicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comprovante não encontrado."));
        boolean canView = entry.getRecipient().getId().equals(user.getId()) ||
                (entry.getSender() != null && entry.getSender().getId().equals(user.getId()));
        if (!canView) throw new ApiException(HttpStatus.NOT_FOUND, "Comprovante não encontrado.");
        return toView(entry, user.getId());
    }

    @Transactional(readOnly = true)
    public List<EntryView> statement(String senderEmail, int limit) {
        UserAccount user = auth.requireUser(senderEmail);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return ledger.findAccountStatement(user.getId(), PageRequest.of(0, safeLimit))
                .getContent().stream().map(entry -> toView(entry, user.getId())).toList();
    }

    @Transactional(readOnly = true)
    public DashboardView dashboard(String senderEmail) {
        UserAccount user = auth.requireUser(senderEmail);
        YearMonth now = YearMonth.now(BRAZIL);
        YearMonth initial = now.minusMonths(5);
        Instant from = initial.atDay(1).atStartOfDay(BRAZIL).toInstant();
        Map<YearMonth, MutableMonth> grouped = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) grouped.put(initial.plusMonths(i), new MutableMonth());
        for (LedgerEntry entry : ledger.findSince(user.getId(), from)) {
            YearMonth month = YearMonth.from(entry.getCreatedAt().atZone(BRAZIL));
            MutableMonth m = grouped.get(month);
            if (m == null) continue;
            m.count++;
            if (entry.getSender() != null && entry.getSender().getId().equals(user.getId())) m.expense += entry.getAmountCents();
            else m.income += entry.getAmountCents();
        }
        List<MonthView> months = grouped.entrySet().stream().map(e -> new MonthView(
                e.getKey().toString(),
                e.getKey().format(DateTimeFormatter.ofPattern("MMM", PT_BR)).replace(".", ""),
                e.getValue().income, e.getValue().expense, e.getValue().count)).toList();
        MutableMonth thisMonth = grouped.get(now);
        List<EntryView> recent = ledger.findAccountStatement(user.getId(), PageRequest.of(0, 5))
                .getContent().stream().map(entry -> toView(entry, user.getId())).toList();
        return new DashboardView(AuthService.UserView.of(user), thisMonth.income, thisMonth.expense, months, recent);
    }

    private static class MutableMonth { long income; long expense; int count; }

    private static EntryView toView(LedgerEntry entry, Long viewer) {
        boolean outgoing = entry.getSender() != null && entry.getSender().getId().equals(viewer);
        String other = entry.getType() == LedgerEntry.Type.DEMO_DEPOSIT ? "Depósito demonstrativo" :
                outgoing ? entry.getRecipient().getFullName() : entry.getSender().getFullName();
        return new EntryView(entry.getPublicId(), entry.getType().name(), outgoing ? "OUT" : "IN",
                outgoing ? -entry.getAmountCents() : entry.getAmountCents(), other, entry.getDescription(),
                entry.getCreatedAt(), entry.getSender() == null ? "Origem demonstrativa" : entry.getSender().getFullName(),
                entry.getRecipient().getFullName());
    }

    private Long requireUserId(String email) {
        return accounts.findIdByEmail(AuthService.normalizeEmail(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));
    }

    private Long findRecipientId(String identifier) {
        if (identifier == null || identifier.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um e-mail ou telefone.");
        String value = identifier.trim();
        if (value.length() > 180) throw new ApiException(HttpStatus.BAD_REQUEST, "Identificador inválido.");
        Optional<Long> found = value.contains("@") ? accounts.findIdByEmail(AuthService.normalizeEmail(value))
                : accounts.findIdByPhone(PhoneNumber.normalize(value));
        return found.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "Nenhuma conta Nuvia encontrada com esses dados."));
    }

    private UserAccount findRecipient(String identifier) {
        return accounts.findById(findRecipientId(identifier))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    private UserAccount lock(Long id) {
        return accounts.lockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada."));
    }

    private static long checkAmount(Long cents) {
        if (cents == null || cents < 1 || cents > MAX_OPERATION_CENTS)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um valor entre R$ 0,01 e R$ 100.000,00.");
        return cents;
    }

    private static String checkKey(String raw) {
        try { return UUID.fromString(raw).toString(); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "Identificador da operação inválido."); }
    }

    private static EntryView replayDeposit(LedgerEntry entry, long amount, Long userId) {
        if (entry.getType() != LedgerEntry.Type.DEMO_DEPOSIT || entry.getAmountCents() != amount)
            throw new ApiException(HttpStatus.CONFLICT, "Esta operação já foi utilizada com outros dados.");
        return toView(entry, userId);
    }
    private static EntryView replayTransfer(LedgerEntry entry, long amount, Long senderId, Long recipientId) {
        if (entry.getType() != LedgerEntry.Type.INTERNAL_PIX || entry.getAmountCents() != amount ||
                entry.getSender() == null || !entry.getSender().getId().equals(senderId) ||
                !entry.getRecipient().getId().equals(recipientId))
            throw new ApiException(HttpStatus.CONFLICT, "Esta operação já foi utilizada com outros dados.");
        return toView(entry, senderId);
    }
}
