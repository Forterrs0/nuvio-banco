package br.com.nuvia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import java.util.Map;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/favicon.svg", "/robots.txt", "/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email").passwordParameter("password")
                .successHandler((req, res, authentication) -> writeJson(res, mapper, 200, Map.of("message", "Autenticado.")))
                .failureHandler((req, res, ex) -> writeJson(res, mapper, 401, Map.of("message", "E-mail ou senha incorretos."))))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> writeJson(res, mapper, 200, Map.of("message", "Sessão encerrada."))))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> writeJson(res, mapper, 401, Map.of("message", "Faça login para continuar.")))
                .accessDeniedHandler((req, res, ex) -> writeJson(res, mapper, 403, Map.of("message", "Acesso negado. Atualize a página e tente novamente."))))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'"))
                .frameOptions(frame -> frame.deny()))
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    private static void writeJson(HttpServletResponse res, ObjectMapper mapper, int status, Object value) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        mapper.writeValue(res.getWriter(), value);
    }
}
