package br.com.nuvia;

import br.com.nuvia.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAccessTest {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;

    @Test
    void onlyAdminCanReadUsersAndSummary() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/users").with(user("cliente@nuvia.test").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/summary").with(user("cliente@nuvia.test").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").with(user("admin@nuvia.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("balanceCents"))));
    }

    @Test
    void newAccountIsCustomerUntilPromotedByDatabaseAdmin() {
        String slug = UUID.randomUUID().toString().substring(0, 8);
        String email = slug + "@nuvia.test";
        String phone = "719" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        auth.register(new AuthService.RegisterRequest("Conta Teste", email, phone, "SenhaForte123"));
        assertFalse(auth.requireUser(email).isAdmin());
        assertTrue(auth.loadUserByUsername(email).getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals));
        jdbc.update("UPDATE user_accounts SET is_admin=TRUE WHERE email=?", email);
        assertTrue(auth.loadUserByUsername(email).getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }
}
