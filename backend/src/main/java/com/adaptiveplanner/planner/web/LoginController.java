package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.account.TeacherAccount;
import com.adaptiveplanner.planner.auth.InvalidOtpRequestException;
import com.adaptiveplanner.planner.auth.EmailRequestLock;
import com.adaptiveplanner.planner.auth.OtpAuthenticationService;
import com.adaptiveplanner.planner.auth.OtpDeliveryUnavailableException;
import com.adaptiveplanner.planner.auth.OtpRateLimitException;
import com.adaptiveplanner.planner.auth.SessionTokenService;
import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {
    private final OtpAuthenticationService authenticationService;
    private final SessionTokenService sessionTokens;
    private final EmailRequestLock emailRequestLock;

    public LoginController(OtpAuthenticationService authenticationService, SessionTokenService sessionTokens,
                           EmailRequestLock emailRequestLock) {
        this.authenticationService = authenticationService;
        this.sessionTokens = sessionTokens;
        this.emailRequestLock = emailRequestLock;
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal TeacherPrincipal principal) {
        return principal == null ? "login" : "redirect:/";
    }

    @PostMapping("/login/request-code")
    public String requestCode(@RequestParam String email, HttpServletRequest request, RedirectAttributes redirect) {
        try {
            String normalizedEmail = emailRequestLock.withEmailLock(
                    email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT),
                    () -> authenticationService.requestCode(email, request.getRemoteAddr()));
            redirect.addFlashAttribute("email", normalizedEmail);
            redirect.addFlashAttribute("message", "If this address can receive email, a sign-in code has been sent. It expires in 5 minutes.");
        } catch (InvalidOtpRequestException exception) {
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (OtpRateLimitException exception) {
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("error", exception.getMessage());
        } catch (OtpDeliveryUnavailableException exception) {
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("error", "Email delivery is not configured. Set up SMTP, then request a new code.");
        }
        return "redirect:/login";
    }

    @PostMapping("/login/verify-code")
    public String verifyCode(@RequestParam String email, @RequestParam String code,
                             HttpServletRequest request, HttpServletResponse response,
                             RedirectAttributes redirect) {
        try {
            TeacherAccount teacher = authenticationService.verifyCode(email, code, request.getRemoteAddr());
            sessionTokens.issue(teacher, response);
            return "redirect:/";
        } catch (InvalidOtpRequestException | OtpRateLimitException exception) {
            redirect.addFlashAttribute("email", email);
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        sessionTokens.clear(response);
        return "redirect:/login?loggedOut";
    }
}
