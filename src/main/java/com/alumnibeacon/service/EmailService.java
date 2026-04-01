package com.alumnibeacon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * P4 — Email Notifications
 *
 * Sends HTML emails via SMTP when mail.enabled=true.
 * When disabled (default), logs the email content to console so developers
 * can see exactly what would be sent without needing an SMTP server.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender    mailSender;
    private final TemplateEngine    templateEngine;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${mail.from:noreply@alumnibeacon.com}")
    private String mailFrom;

    @Value("${mail.from-name:AlumniBeacon}")
    private String mailFromName;

    // ─────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────

    /**
     * Sends a password-reset email.
     *
     * @param toEmail   recipient email address
     * @param toName    recipient display name
     * @param resetUrl  the full password-reset URL
     */
    public void sendPasswordReset(String toEmail, String toName, String resetUrl) {
        Context ctx = new Context();
        ctx.setVariable("name",     toName);
        ctx.setVariable("resetUrl", resetUrl);
        ctx.setVariable("expiryHours", 1);

        send(
            toEmail,
            toName,
            "Reset your AlumniBeacon password",
            "email/password-reset",
            ctx,
            Map.of("resetUrl", resetUrl)
        );
    }

    /**
     * Sends an investigation-completion notification.
     *
     * @param toEmail         recipient email address
     * @param toName          recipient display name
     * @param subjectName     the investigated subject's full name
     * @param investigationId the investigation UUID
     * @param confidenceScore 0-100 confidence score
     * @param engine          which engine ran the investigation (python / agent-zero)
     * @param baseUrl         application base URL for the detail link
     */
    public void sendInvestigationComplete(String toEmail,
                                          String toName,
                                          String subjectName,
                                          String investigationId,
                                          int    confidenceScore,
                                          String engine,
                                          String baseUrl) {
        String detailUrl = baseUrl + "/investigations/" + investigationId;

        Context ctx = new Context();
        ctx.setVariable("name",            toName);
        ctx.setVariable("subjectName",     subjectName);
        ctx.setVariable("confidenceScore", confidenceScore);
        ctx.setVariable("engine",          engine);
        ctx.setVariable("detailUrl",       detailUrl);
        ctx.setVariable("engineLabel",     engineLabel(engine));

        send(
            toEmail,
            toName,
            "Investigation complete: " + subjectName,
            "email/investigation-complete",
            ctx,
            Map.of("detailUrl", detailUrl, "subjectName", subjectName)
        );
    }

    /**
     * Sends an investigation-failure notification.
     *
     * @param toEmail         recipient email address
     * @param toName          recipient display name
     * @param subjectName     the investigated subject's full name
     * @param investigationId the investigation UUID
     * @param errorMessage    the error message
     * @param baseUrl         application base URL
     */
    public void sendInvestigationFailed(String toEmail,
                                        String toName,
                                        String subjectName,
                                        String investigationId,
                                        String errorMessage,
                                        String baseUrl) {
        String listUrl = baseUrl + "/investigations";

        Context ctx = new Context();
        ctx.setVariable("name",         toName);
        ctx.setVariable("subjectName",  subjectName);
        ctx.setVariable("errorMessage", errorMessage);
        ctx.setVariable("listUrl",      listUrl);

        send(
            toEmail,
            toName,
            "Investigation failed: " + subjectName,
            "email/investigation-failed",
            ctx,
            Map.of("listUrl", listUrl, "subjectName", subjectName)
        );
    }

    /** Returns true if email sending is enabled. */
    public boolean isEnabled() {
        return mailEnabled;
    }

    // ─────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────

    private void send(String toEmail,
                      String toName,
                      String subject,
                      String templateName,
                      Context ctx,
                      Map<String, Object> logVars) {

        String htmlBody = templateEngine.process(templateName, ctx);

        if (!mailEnabled) {
            // DEV MODE — log what would be sent
            log.info("══════════════════════════════════════════════════");
            log.info("EMAIL (not sent — mail.enabled=false)");
            log.info("  To:      {} <{}>", toName, toEmail);
            log.info("  Subject: {}", subject);
            logVars.forEach((k, v) -> log.info("  {}: {}", k, v));
            log.info("══════════════════════════════════════════════════");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom, mailFromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent: '{}' → {}", subject, toEmail);

        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}: {}", subject, toEmail, e.getMessage());
            // Don't rethrow — email failure should never break the main flow
        }
    }

    private String engineLabel(String engine) {
        if (engine == null) return "Standard";
        return switch (engine) {
            case "agent-zero" -> "Deep Investigation (Agent Zero)";
            case "hybrid"     -> "Hybrid";
            case "python-fallback" -> "Standard (fallback)";
            default           -> "Standard";
        };
    }
}
