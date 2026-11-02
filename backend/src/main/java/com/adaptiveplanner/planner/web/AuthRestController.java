package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.account.TeacherAccount;
import com.adaptiveplanner.planner.auth.OtpAuthenticationService;
import com.adaptiveplanner.planner.auth.SessionTokenService;
import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {
    private final OtpAuthenticationService authenticationService;
    private final SessionTokenService sessionTokens;
    private final CookieCsrfTokenRepository csrfTokens;

    public AuthRestController(OtpAuthenticationService authenticationService, SessionTokenService sessionTokens,
                              CookieCsrfTokenRepository csrfTokens) {
        this.authenticationService = authenticationService;
        this.sessionTokens = sessionTokens;
        this.csrfTokens = csrfTokens;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getToken());
    }

    @PostMapping("/send-otp")
    public MessageResponse sendOtp(@Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        authenticationService.requestCode(body.email(), request.getRemoteAddr());
        return new MessageResponse("If this address can receive email, a sign-in code has been sent.");
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest body, HttpServletRequest request,
                                  HttpServletResponse response) {
        TeacherAccount teacher = authenticationService.verifyCode(body.email(), body.code(), request.getRemoteAddr());
        sessionTokens.issue(teacher, response);
        return new AuthResponse(new UserResponse(teacher.getId(), teacher.getEmail()));
    }

    @GetMapping("/me")
    public AuthResponse currentUser(@AuthenticationPrincipal TeacherPrincipal principal) {
        return new AuthResponse(new UserResponse(principal.id(), principal.email()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        sessionTokens.clear(response);
        csrfTokens.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    public record EmailRequest(
            @NotBlank @Email @Size(max = 254) String email
    ) { }

    public record VerifyOtpRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{6}") String code
    ) { }

    public record CsrfResponse(String token) { }
    public record MessageResponse(String message) { }
    public record UserResponse(String id, String email) { }
    public record AuthResponse(UserResponse user) { }
}
