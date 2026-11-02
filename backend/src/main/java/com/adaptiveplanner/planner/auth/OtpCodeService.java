package com.adaptiveplanner.planner.auth;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class OtpCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private final SecretKey hashKey;

    public OtpCodeService(AuthProperties properties) {
        this.hashKey = properties.otpHashKey();
    }

    public String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isValidEmail(String email) {
        return email != null && email.length() <= 254 && EMAIL.matcher(email).matches();
    }

    public String generate() {
        return Integer.toString(100_000 + RANDOM.nextInt(900_000));
    }

    public String hash(String normalizedEmail, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hashKey);
            return HexFormat.of().formatHex(mac.doFinal((normalizedEmail + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not process a sign-in code.", exception);
        }
    }

    public boolean matches(String normalizedEmail, String code, String expectedHash) {
        byte[] actual = hash(normalizedEmail, code).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash == null ? new byte[0] : expectedHash.getBytes(StandardCharsets.US_ASCII);
        return actual.length == expected.length && MessageDigest.isEqual(actual, expected);
    }
}
