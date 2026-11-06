package br.com.nuvia;

import br.com.nuvia.auth.AuthService;
import br.com.nuvia.transfer.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReceiptPdfAccessTest {
    @Autowired MockMvc mvc;
    @Autowired BankingService banking;
    @Autowired AuthService auth;

    private String register(String name, String phone) {
        String email = UUID.randomUUID().toString().substring(0,10)+"@nuvia.test";
        auth.register(new AuthService.RegisterRequest(name,email,phone,"SenhaForte123"));
        return email;
    }

    @Test
    void receiptPdfRequiresLoginAndTransactionParticipation() throws Exception {
        String sender = register("Rafael Mota", "(71) 99111-1101");
        String receiver = register("Maria Silva", "(71) 99111-1102");
        String outsider = register("Outra Pessoa", "(71) 99111-1103");
        banking.depositDemo(sender, new BankingService.DepositRequest(2000L, UUID.randomUUID().toString()));
        var transfer = banking.transfer(sender,new BankingService.TransferRequest(receiver,700L,"Almoco",UUID.randomUUID().toString()));
        String path = "/api/transactions/" + transfer.id() + "/receipt.pdf";

        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user(outsider).roles("CUSTOMER")))
                .andExpect(status().isNotFound());
        for (String allowed : new String[]{sender,receiver}) {
            byte[] pdf = mvc.perform(get(path).with(user(allowed).roles("CUSTOMER")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                    .andReturn().getResponse().getContentAsByteArray();
            assertTrue(new String(pdf, 0, Math.min(pdf.length, 5), StandardCharsets.US_ASCII).startsWith("%PDF-"));
            assertTrue(pdf.length > 1000);
        }
    }
}
