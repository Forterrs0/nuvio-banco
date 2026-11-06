package br.com.nuvia.auth;

import br.com.nuvia.account.AccountRepository;
import br.com.nuvia.account.PhoneNumber;
import br.com.nuvia.account.UserAccount;
import br.com.nuvia.api.ApiException;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AuthService implements UserDetailsService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public AuthService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public record RegisterRequest(
            @NotBlank(message = "Informe seu nome.") @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres.") String name,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") String email,
            @NotBlank(message = "Informe seu telefone.") String phone,
            @NotBlank(message = "Informe sua senha.") @Size(min = 8, max = 72, message = "A senha precisa ter entre 8 e 72 caracteres.") String password
    ) {}

    public record UserView(Long id, String name, String email, String phone, long balanceCents, boolean admin) {
        public static UserView of(UserAccount user) {
            return new UserView(user.getId(), user.getFullName(), user.getEmail(),
                    user.getPhone(), user.getBalanceCents(), user.isAdmin());
        }
    }

    @Transactional
    public UserView register(RegisterRequest request) {
        String name = request.name().trim().replaceAll("\\s+", " ");
        String email = normalizeEmail(request.email());
        String phone = PhoneNumber.normalize(request.phone());
        if (name.length() < 3) throw new ApiException(HttpStatus.BAD_REQUEST, "Informe seu nome completo.");
        if (accounts.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Este e-mail já tem uma conta.");
        if (accounts.existsByPhone(phone)) throw new ApiException(HttpStatus.CONFLICT, "Este telefone já tem uma conta.");
        return UserView.of(accounts.save(new UserAccount(name, email, phone, encoder.encode(request.password()))));
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(String email) {
        return accounts.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre novamente."));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount user = accounts.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
        return User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles(user.isAdmin() ? "ADMIN" : "CUSTOMER").build();
    }

    public static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
