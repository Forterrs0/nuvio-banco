package br.com.nuvia.admin;

import br.com.nuvia.account.AccountRepository;
import br.com.nuvia.account.UserAccount;
import br.com.nuvia.api.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/** Endpoints are protected by ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AccountRepository accounts;

    public AdminController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public record AdminUser(long id, String name, String email, String phone,
                            Instant createdAt, boolean admin) {
        static AdminUser of(UserAccount user) {
            return new AdminUser(user.getId(), user.getFullName(), user.getEmail(),
                    user.getPhone(), user.getCreatedAt(), user.isAdmin());
        }
    }

    public record UsersPage(List<AdminUser> users, long total, int totalPages,
                            int page, int size) {}

    public record Summary(long totalUsers, long administrators, long newLast30Days) {}

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public ResponseEntity<UsersPage> users(@RequestParam(defaultValue = "") String search,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || page > 100_000 || size < 1 || size > 50 || search.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Parâmetros de consulta inválidos.");
        }
        String query = search.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        String filter = query.isEmpty() ? "" : "%" + query + "%";
        Page<UserAccount> results = accounts.findForAdmin(filter,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        UsersPage body = new UsersPage(results.getContent().stream().map(AdminUser::of).toList(),
                results.getTotalElements(), results.getTotalPages(), page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<Summary> summary() {
        Summary summary = new Summary(accounts.count(), accounts.countByAdminTrue(),
                accounts.countByCreatedAtGreaterThanEqual(Instant.now().minus(30, ChronoUnit.DAYS)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(summary);
    }
}
