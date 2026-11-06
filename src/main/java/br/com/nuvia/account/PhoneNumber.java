package br.com.nuvia.account;

import br.com.nuvia.api.ApiException;
import org.springframework.http.HttpStatus;

public final class PhoneNumber {
    private PhoneNumber() {}

    /** Converts Brazilian national phones to country-code +55, digits only. */
    public static String normalize(String raw) {
        if (raw == null) throw invalid();
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            digits = digits.substring(2);
        }
        if (digits.length() != 10 && digits.length() != 11) throw invalid();
        int ddd = Integer.parseInt(digits.substring(0, 2));
        if (ddd < 11 || ddd > 99 || digits.charAt(2) == '0') throw invalid();
        if (digits.length() == 11 && digits.charAt(2) != '9') throw invalid();
        return "55" + digits;
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "Informe um telefone brasileiro com DDD (ex.: 71 99999-9999).");
    }
}
