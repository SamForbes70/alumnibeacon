package com.alumnibeacon.controller;

import com.alumnibeacon.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // ─────────────────────────────────────────────────────────────
    // STEP 1 — Forgot Password (enter email)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email,
                                       HttpServletRequest request,
                                       Model model) {
        String baseUrl = getBaseUrl(request);
        String resetUrl = passwordResetService.generateResetToken(email.trim(), baseUrl);

        // Always show the same success message to prevent email enumeration.
        // In dev mode we also surface the reset URL on screen for convenience.
        model.addAttribute("submitted", true);
        model.addAttribute("email", email);

        // DEV CONVENIENCE: show the link on screen until P4 (email) is wired
        if (resetUrl != null) {
            model.addAttribute("devResetUrl", resetUrl);
        }

        return "auth/forgot-password";
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 2 — Reset Password (enter new password)
    // ─────────────────────────────────────────────────────────────

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token,
                                    Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Missing reset token. Please request a new password reset.");
            return "auth/reset-password";
        }

        boolean valid = passwordResetService.validateToken(token).isPresent();
        if (!valid) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
        } else {
            model.addAttribute("token", token);
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        // Client-side validation backup
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match.");
            return "auth/reset-password";
        }

        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(token, password);
            return "redirect:/login?reset=success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/reset-password";
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        // Omit default ports
        if (("http".equals(scheme) && port == 80) ||
            ("https".equals(scheme) && port == 443)) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }
}
