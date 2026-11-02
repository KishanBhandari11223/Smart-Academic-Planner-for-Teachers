package com.adaptiveplanner.planner.auth;

import com.adaptiveplanner.planner.account.TeacherAccount;
import com.adaptiveplanner.planner.account.TeacherAccountRepository;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class OtpAuthenticationService {
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration REQUEST_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_EMAIL_REQUESTS = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private final OtpChallengeRepository challenges;
    private final TeacherAccountRepository teachers;
    private final OtpCodeService codes;
    private final OtpDeliveryService delivery;
    private final RequestRateLimiter rateLimiter;

    public OtpAuthenticationService(OtpChallengeRepository challenges,
                                    TeacherAccountRepository teachers,
                                    OtpCodeService codes,
                                    OtpDeliveryService delivery,
                                    RequestRateLimiter rateLimiter) {
        this.challenges = challenges;
        this.teachers = teachers;
        this.codes = codes;
        this.delivery = delivery;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public String requestCode(String submittedEmail, String clientAddress) {
        String email = codes.normalizeEmail(submittedEmail);
        if (!codes.isValidEmail(email)) {
            throw new InvalidOtpRequestException("Enter a valid email address.");
        }
        rateLimiter.check("request-code", clientAddress, 20, REQUEST_WINDOW);

        Instant now = Instant.now();
        Optional<OtpChallenge> latest = challenges.findTopByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(email);
        if (latest.isPresent() && latest.get().getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN))) {
            throw new OtpRateLimitException();
        }
        if (challenges.countByEmailAndCreatedAtAfter(email, now.minus(REQUEST_WINDOW)) >= MAX_EMAIL_REQUESTS) {
            throw new OtpRateLimitException();
        }

        challenges.invalidateOutstanding(email, now);
        String code = codes.generate();
        challenges.saveAndFlush(new OtpChallenge(email, codes.hash(email, code), now.plus(CODE_LIFETIME)));
        try {
            delivery.sendCode(email, code);
        } catch (MailException | OtpDeliveryUnavailableException exception) {
            throw new OtpDeliveryUnavailableException();
        }
        return email;
    }

    @Transactional(noRollbackFor = InvalidOtpRequestException.class)
    public TeacherAccount verifyCode(String submittedEmail, String submittedCode, String clientAddress) {
        String email = codes.normalizeEmail(submittedEmail);
        String code = submittedCode == null ? "" : submittedCode.trim();
        if (!codes.isValidEmail(email) || !CODE_PATTERN.matcher(code).matches()) {
            throw new InvalidOtpRequestException("Enter a valid email address and six-digit code.");
        }
        rateLimiter.check("verify-code", clientAddress, 30, REQUEST_WINDOW);

        Instant now = Instant.now();
        OtpChallenge challenge = challenges.findTopByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> invalidCode());
        if (!challenge.getExpiresAt().isAfter(now) || challenge.getAttempts() >= MAX_ATTEMPTS) {
            throw invalidCode();
        }

        if (!codes.matches(email, code, challenge.getCodeHash())) {
            challenges.recordFailedAttempt(challenge.getId(), MAX_ATTEMPTS, now);
            throw invalidCode();
        }

        if (challenges.consumeIfActive(challenge.getId(), MAX_ATTEMPTS, now) != 1) {
            throw invalidCode();
        }

        return teachers.findByEmail(email).orElseGet(() -> teachers.save(new TeacherAccount(email)));
    }

    private InvalidOtpRequestException invalidCode() {
        return new InvalidOtpRequestException("That code is invalid or expired. Request a new code and try again.");
    }
}
