package br.com.nuvia.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @GetMapping("/api/auth/csrf")
    Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken(), "headerName", csrfToken.getHeaderName());
    }

    @PostMapping("/api/auth/register")
    ResponseEntity<AuthService.UserView> register(@Valid @RequestBody AuthService.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @GetMapping("/api/me")
    AuthService.UserView me(Authentication authentication) {
        return AuthService.UserView.of(service.requireUser(authentication.getName()));
    }
}
