package br.com.nuvia;

import br.com.nuvia.auth.AuthService;
import br.com.nuvia.account.AccountRepository;
import br.com.nuvia.transfer.BankingService;
import br.com.nuvia.transfer.LedgerRepository;
import br.com.nuvia.api.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BankingServiceTest {
    @Autowired BankingService banking;
    @Autowired AuthService auth;
    @Autowired AccountRepository accounts;
    @Autowired LedgerRepository ledger;

    private String key() { return UUID.randomUUID().toString(); }
    private String register(String name, String phone) {
        String email = UUID.randomUUID().toString().substring(0,10) + "@nuvia.test";
        auth.register(new AuthService.RegisterRequest(name,email,phone,"SenhaForte123"));
        return email;
    }

    @Test void transfersByEmailAndPhoneAreAtomicAndVisibleToBoth() {
        String matheus = register("Matheus Silva", "(71) 99999-1234");
        String jose = register("Jose Carvalho", "(71) 99999-5678");
        banking.depositDemo(matheus,new BankingService.DepositRequest(12_000L,key()));
        var sent = banking.transfer(matheus,new BankingService.TransferRequest(jose,5_075L,"Teste",key()));
        assertEquals(-5_075L,sent.amountCents());
        assertEquals(6_925L,auth.requireUser(matheus).getBalanceCents());
        assertEquals(5_075L,auth.requireUser(jose).getBalanceCents());
        assertEquals(5_075L,banking.statement(jose,10).getFirst().amountCents());
        assertEquals("Jose Carvalho",banking.resolveRecipient(matheus,"71 99999-5678").name());
        assertEquals(sent.id(),banking.receipt(jose,sent.id()).id());
    }

    @Test void duplicateIdempotencyKeyDoesNotDuplicateMoney() {
        String from = register("Ana Maria", "(71) 98888-3333");
        String to = register("Joao Pedro", "(71) 98888-4444");
        var dep = new BankingService.DepositRequest(1000L,key());
        banking.depositDemo(from,dep);
        banking.depositDemo(from,dep);
        assertEquals(1000L,auth.requireUser(from).getBalanceCents());

        var transfer = new BankingService.TransferRequest(to, 700L,"Presente",key());
        var first = banking.transfer(from,transfer);
        var replay = banking.transfer(from,transfer);
        assertEquals(first.id(),replay.id());
        assertEquals(300L,auth.requireUser(from).getBalanceCents());
        assertEquals(700L,auth.requireUser(to).getBalanceCents());
        assertEquals(2,ledger.count());
    }

    @Test void insufficientBalanceAndOwnAccountAreRejected() {
        String from = register("Bia Costa", "(71) 97777-1111");
        String to = register("Leo Santos", "(71) 97777-2222");
        ApiException insufficient = assertThrows(ApiException.class, () -> banking.transfer(from,
                new BankingService.TransferRequest(to,100L,"",key())));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, insufficient.getStatus());
        assertThrows(ApiException.class, () -> banking.transfer(from,
                new BankingService.TransferRequest(from,100L,"",key())));
        assertEquals(0L,auth.requireUser(from).getBalanceCents());
        assertEquals(0L,auth.requireUser(to).getBalanceCents());
    }

    @Test void reusedKeyWithDifferentAmountIsRejected() {
        String from = register("Iara Mendes", "(71) 96666-1111");
        banking.depositDemo(from,new BankingService.DepositRequest(100L,"00000000-0000-0000-0000-000000000001"));
        ApiException error = assertThrows(ApiException.class, () -> banking.depositDemo(from,
                new BankingService.DepositRequest(999L,"00000000-0000-0000-0000-000000000001")));
        assertEquals(HttpStatus.CONFLICT,error.getStatus());
        assertEquals(100L,auth.requireUser(from).getBalanceCents());
    }
}
