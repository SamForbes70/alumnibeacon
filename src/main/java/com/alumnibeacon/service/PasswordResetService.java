package com.alumnibeacon.service;

import com.alumnibeacon.model.PasswordResetToken;
import com.alumnibeacon.model.User;
import com.alumnibeacon.repository.PasswordResetTokenRepository;
import com.alumnibeacon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository               userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder              passwordEncoder;
    private final EmailService                 emailService;

    /** Token validity window (1 hour). */
    private static final int TOKEN_EXPIRY_HOURS = 1;

    /**
     * Generates a password-reset token for the given email address.
     * Silently succeeds even if the email is not registered (prevents enumeration).
     *
     * @param email     the user's email address
     * @param baseUrl   the application base URL (e.g. http://localhost:8080)
     * @return the full reset URL, or null if the email was not found
     */
    @Transactional
    public String generateResetToken(String email, String baseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            // Don't reveal whether the email exists
            log.info("Password reset requested for unknown email: {}", email);
            return null;
        }

        User user = userOpt.get();

        // Invalidate any existing unused tokens for this user
        tokenRepository.invalidateAllForUser(user.getId());

        // Generate a new secure token (64-char hex, 256-bit entropy)
        String rawToken = UUID.randomUUID().toString().replace("-", "") +
                          UUID.randomUUID().toString().replace("-", "");

        PasswordResetToken prt = PasswordResetToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();
        tokenRepository.save(prt);

        String resetUrl = baseUrl + "/reset-password?token=" + rawToken;

        // Send email (real SMTP when mail.enabled=true, logged to console otherwise)
        emailService.sendPasswordReset(user.getEmail(), user.getFullName(), resetUrl);

        // Always log the URL — useful in dev even when email is enabled
        log.info("Password reset token generated for: {}", email);
        if (!emailService.isEnabled()) {
            log.info("==================================================");
            log.info("DEV MODE — PASSWORD RESET LINK for {}:", email);
            log.info("{}", resetUrl);
            log.info("==================================================");
        }

        return resetUrl;
    }

    /**
     * Validates a reset token.
     *
     * @param token the raw token string from the URL
     * @return the token entity if valid, empty otherwise
     */
    public Optional<PasswordResetToken> validateToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(PasswordResetToken::isValid);
    }

    /**
     * Resets the user's password using a valid token.
     *
     * @param token       the raw token string
     * @param newPassword the new plain-text password
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link."));

        if (!prt.isValid()) {
            throw new IllegalArgumentException("This reset link has expired or already been used.");
        }

        User user = userRepository.findById(prt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        prt.setUsed(true);
        tokenRepository.save(prt);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
