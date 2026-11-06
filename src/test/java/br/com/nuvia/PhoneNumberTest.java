package br.com.nuvia;

import br.com.nuvia.account.PhoneNumber;
import br.com.nuvia.api.ApiException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberTest {
    @Test void nationalAndInternationalNumbersMatch() {
        assertEquals("5571999991234",PhoneNumber.normalize("(71) 99999-1234"));
        assertEquals("5571999991234",PhoneNumber.normalize("+55 71 99999-1234"));
        assertEquals("557133334444",PhoneNumber.normalize("71 3333-4444"));
    }
    @Test void invalidPhoneIsRejected() {
        assertThrows(ApiException.class,()->PhoneNumber.normalize("123"));
        assertThrows(ApiException.class,()->PhoneNumber.normalize("00 99999-9999"));
    }
}
