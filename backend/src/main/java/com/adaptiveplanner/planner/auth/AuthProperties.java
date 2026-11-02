package com.adaptiveplanner.planner.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String jwtSecretBase64;
    private String otpHashSecretBase64;
    private String issuer;
    private String audience;
    private boolean cookieSecure;
    private Duration sessionDuration = Duration.ofDays(7);

    public SecretKey jwtKey() { return decodeKey(jwtSecretBase64, "JWT_SECRET_BASE64"); }
    public SecretKey otpHashKey() { return decodeKey(otpHashSecretBase64, "OTP_HASH_SECRET_BASE64"); }

    private SecretKey decodeKey(String encoded, String variableName) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Required authentication secret is missing: " + variableName);
        }
        try {
            byte[] key = Base64.getDecoder().decode(encoded);
            if (key.length < 32) {
                throw new IllegalStateException(variableName + " must decode to at least 32 bytes.");
            }
            return new SecretKeySpec(key, "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(variableName + " must be valid Base64.");
        }
    }

    public String getJwtSecretBase64() { return jwtSecretBase64; }
    public void setJwtSecretBase64(String jwtSecretBase64) { this.jwtSecretBase64 = jwtSecretBase64; }
    public String getOtpHashSecretBase64() { return otpHashSecretBase64; }
    public void setOtpHashSecretBase64(String otpHashSecretBase64) { this.otpHashSecretBase64 = otpHashSecretBase64; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public boolean isCookieSecure() { return cookieSecure; }
    public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
    public Duration getSessionDuration() { return sessionDuration; }
    public void setSessionDuration(Duration sessionDuration) { this.sessionDuration = sessionDuration; }
}
