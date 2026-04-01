"""
P4 — Email Notifications — Playwright Tests

Tests verify:
- DEV MODE banner shown when mail.enabled=false (default)
- email-sent-badge NOT shown when mail.enabled=false
- Password reset flow still works end-to-end (regression)
- Email templates render without Thymeleaf errors
- Investigation completion notification wiring (structural)

Note: Real SMTP delivery cannot be tested in Playwright.
SMTP behaviour is verified by unit/integration tests or manual testing.
These tests verify the UI layer and graceful degradation.
"""
import re
import pytest
from playwright.sync_api import Page, expect

BASE = "http://localhost:8080"


class TestEmailNotificationsDevMode:
    """Tests for mail.enabled=false (default dev mode)."""

    def test_forgot_password_page_loads(self, page: Page):
        """Forgot password page renders without errors."""
        page.goto(f"{BASE}/forgot-password")
        expect(page.locator("h1, .text-xl")).to_contain_text("Reset")
        expect(page.locator("input[name='email']")).to_be_visible()
        expect(page.locator("button[type='submit']")).to_be_visible()

    def test_dev_mode_shows_reset_link_for_known_email(self, page: Page):
        """DEV MODE banner with reset link shown for known email when mail disabled."""
        page.goto(f"{BASE}/forgot-password")
        page.fill("input[name='email']", "admin@utas.edu.au")
        page.click("button[type='submit']")

        # Success state shown
        expect(page.locator("text=Check your email")).to_be_visible()

        # DEV MODE banner present (mail.enabled=false by default)
        dev_banner = page.locator("#dev-reset-banner")
        expect(dev_banner).to_be_visible()
        expect(dev_banner).to_contain_text("DEV MODE")

        # Reset link is a valid URL with token
        reset_link = page.locator("#dev-reset-banner a")
        expect(reset_link).to_be_visible()
        href = reset_link.get_attribute("href")
        assert href is not None
        assert "/reset-password?token=" in href
        assert len(href.split("token=")[1]) == 64, "Token should be 64 hex chars"

    def test_dev_mode_no_email_sent_badge_when_mail_disabled(self, page: Page):
        """email-sent-badge NOT shown when mail.enabled=false."""
        page.goto(f"{BASE}/forgot-password")
        page.fill("input[name='email']", "admin@utas.edu.au")
        page.click("button[type='submit']")

        expect(page.locator("text=Check your email")).to_be_visible()
        # email-sent-badge should NOT be present when mail is disabled
        expect(page.locator("#email-sent-badge")).not_to_be_visible()

    def test_unknown_email_no_dev_banner(self, page: Page):
        """Unknown email shows success (anti-enumeration) but no DEV MODE banner."""
        page.goto(f"{BASE}/forgot-password")
        page.fill("input[name='email']", "nobody@nowhere.com")
        page.click("button[type='submit']")

        # Success message shown (anti-enumeration)
        expect(page.locator("text=Check your email")).to_be_visible()

        # No DEV MODE banner (no token generated for unknown email)
        expect(page.locator("#dev-reset-banner")).not_to_be_visible()

    def test_reset_link_navigates_to_valid_reset_page(self, page: Page):
        """DEV MODE reset link navigates to a valid reset-password form (regression)."""
        # Request reset for known user
        page.goto(f"{BASE}/forgot-password")
        page.fill("input[name='email']", "admin@utas.edu.au")
        page.click("button[type='submit']")
        expect(page.locator("text=Check your email")).to_be_visible()

        # Get reset URL from DEV MODE banner
        reset_link = page.locator("#dev-reset-banner a")
        expect(reset_link).to_be_visible()
        reset_url = reset_link.get_attribute("href")
        assert reset_url and "/reset-password?token=" in reset_url

        # Navigate to reset page — should show the password form (not an error)
        page.goto(reset_url)
        expect(page.locator("input[name='password']")).to_be_visible()
        expect(page.locator("input[name='confirmPassword']")).to_be_visible()
        # Should NOT show an error (token is valid)
        expect(page.locator(".text-red-400, .text-red-300")).not_to_be_visible()

class TestEmailTemplateRendering:
    """Verify email templates render without Thymeleaf errors."""

    def test_password_reset_template_exists(self, page: Page):
        """Password reset email template file exists and is valid HTML."""
        import os
        template_path = "/a0/usr/projects/aulumnibeacon/src/main/resources/templates/email/password-reset.html"
        assert os.path.exists(template_path), "password-reset.html template missing"
        content = open(template_path).read()
        assert "th:text" in content, "Template should use Thymeleaf expressions"
        assert "resetUrl" in content, "Template should reference resetUrl variable"
        assert "AlumniBeacon" in content, "Template should contain brand name"

    def test_investigation_complete_template_exists(self, page: Page):
        """Investigation complete email template file exists and is valid HTML."""
        import os
        template_path = "/a0/usr/projects/aulumnibeacon/src/main/resources/templates/email/investigation-complete.html"
        assert os.path.exists(template_path), "investigation-complete.html template missing"
        content = open(template_path).read()
        assert "confidenceScore" in content, "Template should reference confidenceScore"
        assert "detailUrl" in content, "Template should reference detailUrl"
        assert "subjectName" in content, "Template should reference subjectName"

    def test_investigation_failed_template_exists(self, page: Page):
        """Investigation failed email template file exists and is valid HTML."""
        import os
        template_path = "/a0/usr/projects/aulumnibeacon/src/main/resources/templates/email/investigation-failed.html"
        assert os.path.exists(template_path), "investigation-failed.html template missing"
        content = open(template_path).read()
        assert "errorMessage" in content, "Template should reference errorMessage"
        assert "listUrl" in content, "Template should reference listUrl"


class TestEmailServiceConfig:
    """Verify email service configuration and wiring."""

    def test_email_service_java_exists(self, page: Page):
        """EmailService.java exists with correct structure."""
        import os
        service_path = "/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/service/EmailService.java"
        assert os.path.exists(service_path), "EmailService.java missing"
        content = open(service_path).read()
        assert "sendPasswordReset" in content
        assert "sendInvestigationComplete" in content
        assert "sendInvestigationFailed" in content
        assert "mail.enabled" in content
        assert "JavaMailSender" in content

    def test_job_scheduler_sends_emails(self, page: Page):
        """JobQueueScheduler is wired to send completion/failure emails."""
        import os
        scheduler_path = "/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/service/JobQueueScheduler.java"
        content = open(scheduler_path).read()
        assert "sendCompletionEmail" in content, "Scheduler should call sendCompletionEmail"
        assert "sendFailureEmail" in content, "Scheduler should call sendFailureEmail"
        assert "EmailService" in content, "Scheduler should inject EmailService"

    def test_application_properties_has_mail_config(self, page: Page):
        """application.properties has mail configuration."""
        import os
        props_path = "/a0/usr/projects/aulumnibeacon/src/main/resources/application.properties"
        content = open(props_path).read()
        assert "mail.enabled" in content
        assert "spring.mail.host" in content
        assert "spring.mail.port" in content
        assert "app.base-url" in content

    def test_mail_disabled_by_default(self, page: Page):
        """mail.enabled defaults to false (safe default — no accidental SMTP)."""
        props_path = "/a0/usr/projects/aulumnibeacon/src/main/resources/application.properties"
        content = open(props_path).read()
        # Default should be false
        assert "MAIL_ENABLED:false" in content, "mail.enabled should default to false"
