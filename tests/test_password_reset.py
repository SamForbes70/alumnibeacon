"""
P5 — Password Reset Playwright Tests
Covers: forgot-password page, reset-password page, full reset flow,
        token reuse prevention, password mismatch validation,
        login page forgot-password link, reset success banner.
"""
import re
import pytest
from playwright.sync_api import Page, expect


# ─────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────

def request_reset_token(page: Page, email: str) -> str:
    """Submit forgot-password form and extract the dev reset URL token."""
    page.goto("http://localhost:8080/forgot-password")
    page.fill("input[name=email]", email)
    page.click("button[type=submit]")
    page.wait_for_selector("text=Check your email")
    # Extract token from the dev-mode reset link
    link = page.locator("a[href*='reset-password?token=']").first
    href = link.get_attribute("href")
    token = href.split("token=")[1]
    return token


# ─────────────────────────────────────────────────────────────
# T1 — Forgot Password page loads
# ─────────────────────────────────────────────────────────────

def test_forgot_password_page_loads(page: Page):
    page.goto("http://localhost:8080/forgot-password")
    expect(page).to_have_title(re.compile("Forgot Password", re.IGNORECASE))
    expect(page.locator("input[name=email]")).to_be_visible()
    expect(page.locator("button[type=submit]")).to_be_visible()
    expect(page.locator("text=Reset your password")).to_be_visible()
    expect(page.locator("a[href='/login']")).to_be_visible()


# ─────────────────────────────────────────────────────────────
# T2 — Forgot password with unknown email (anti-enumeration)
# ─────────────────────────────────────────────────────────────

def test_forgot_password_unknown_email_shows_success(page: Page):
    """Should show success message even for unknown emails (prevents enumeration)."""
    page.goto("http://localhost:8080/forgot-password")
    page.fill("input[name=email]", "nobody@unknown.com")
    page.click("button[type=submit]")
    # Same success message regardless of whether email exists
    expect(page.locator("text=Check your email")).to_be_visible()
    # No dev reset URL for unknown email
    assert page.locator("a[href*='reset-password?token=']").count() == 0


# ─────────────────────────────────────────────────────────────
# T3 — Forgot password with known email shows dev reset link
# ─────────────────────────────────────────────────────────────

def test_forgot_password_known_email_shows_dev_link(page: Page):
    page.goto("http://localhost:8080/forgot-password")
    page.fill("input[name=email]", "admin@utas.edu.au")
    page.click("button[type=submit]")
    expect(page.locator("text=Check your email")).to_be_visible()
    expect(page.locator("text=DEV MODE")).to_be_visible()
    # Dev reset link is present and clickable
    link = page.locator("a[href*='reset-password?token=']").first
    expect(link).to_be_visible()
    href = link.get_attribute("href")
    assert "token=" in href
    assert len(href.split("token=")[1]) == 64  # 64-char hex token


# ─────────────────────────────────────────────────────────────
# T4 — Reset password page with no token shows error
# ─────────────────────────────────────────────────────────────

def test_reset_password_no_token_shows_error(page: Page):
    page.goto("http://localhost:8080/reset-password")
    expect(page.locator("text=Link invalid or expired")).to_be_visible()
    expect(page.locator("a[href='/forgot-password']")).to_be_visible()
    # Form should NOT be shown
    assert page.locator("input[name=password]").count() == 0


# ─────────────────────────────────────────────────────────────
# T5 — Reset password page with invalid token shows error
# ─────────────────────────────────────────────────────────────

def test_reset_password_invalid_token_shows_error(page: Page):
    page.goto("http://localhost:8080/reset-password?token=invalidtoken123")
    expect(page.locator("text=Link invalid or expired")).to_be_visible()
    expect(page.locator("a[href='/forgot-password']")).to_be_visible()
    assert page.locator("input[name=password]").count() == 0


# ─────────────────────────────────────────────────────────────
# T6 — Reset password page with valid token shows form
# ─────────────────────────────────────────────────────────────

def test_reset_password_valid_token_shows_form(page: Page):
    token = request_reset_token(page, "admin@utas.edu.au")
    page.goto(f"http://localhost:8080/reset-password?token={token}")
    expect(page.locator("text=Set a new password")).to_be_visible()
    expect(page.locator("input[name=password]")).to_be_visible()
    expect(page.locator("input[name=confirmPassword]")).to_be_visible()
    expect(page.locator("button[type=submit]")).to_be_visible()
    # Token is in hidden field
    hidden = page.locator("input[name=token]")
    assert hidden.get_attribute("value") == token


# ─────────────────────────────────────────────────────────────
# T7 — Full reset flow: request → reset → login with new password
# ─────────────────────────────────────────────────────────────

def test_full_password_reset_flow(page: Page):
    # Step 1: Request reset
    token = request_reset_token(page, "admin@utas.edu.au")
    assert len(token) == 64

    # Step 2: Visit reset page
    page.goto(f"http://localhost:8080/reset-password?token={token}")
    expect(page.locator("input[name=password]")).to_be_visible()

    # Step 3: Submit new password
    page.fill("input[name=password]", "TempPassword999!")
    page.fill("input[name=confirmPassword]", "TempPassword999!")
    page.click("button[type=submit]")

    # Step 4: Should redirect to login with reset=success
    page.wait_for_url("**/login**")
    expect(page.locator("text=Password reset successfully")).to_be_visible()

    # Step 5: Login with new password
    page.fill("input[name=email]", "admin@utas.edu.au")
    page.fill("input[name=password]", "TempPassword999!")
    page.click("button[type=submit]")
    page.wait_for_url("**/dashboard**")
    expect(page).to_have_url(re.compile(".*/dashboard.*"))

    # Step 6: Reset password back to original so other tests pass
    token2 = request_reset_token(page, "admin@utas.edu.au")
    page.goto(f"http://localhost:8080/reset-password?token={token2}")
    page.fill("input[name=password]", "password123")
    page.fill("input[name=confirmPassword]", "password123")
    page.click("button[type=submit]")
    page.wait_for_url("**/login**")
    expect(page.locator("text=Password reset successfully")).to_be_visible()


# ─────────────────────────────────────────────────────────────
# T8 — Token reuse is blocked after successful reset
# ─────────────────────────────────────────────────────────────

def test_token_cannot_be_reused(page: Page):
    # Get a token and use it
    token = request_reset_token(page, "admin@utas.edu.au")
    page.goto(f"http://localhost:8080/reset-password?token={token}")
    page.fill("input[name=password]", "TempPassword888!")
    page.fill("input[name=confirmPassword]", "TempPassword888!")
    page.click("button[type=submit]")
    page.wait_for_url("**/login**")

    # Try to reuse the same token
    page.goto(f"http://localhost:8080/reset-password?token={token}")
    expect(page.locator("text=Link invalid or expired")).to_be_visible()

    # Reset back to original password
    token2 = request_reset_token(page, "admin@utas.edu.au")
    page.goto(f"http://localhost:8080/reset-password?token={token2}")
    page.fill("input[name=password]", "password123")
    page.fill("input[name=confirmPassword]", "password123")
    page.click("button[type=submit]")
    page.wait_for_url("**/login**")


# ─────────────────────────────────────────────────────────────
# T9 — Password mismatch shows client-side error
# ─────────────────────────────────────────────────────────────

def test_password_mismatch_shows_error(page: Page):
    token = request_reset_token(page, "admin@utas.edu.au")
    page.goto(f"http://localhost:8080/reset-password?token={token}")
    page.fill("input[name=password]", "Password123!")
    page.fill("input[name=confirmPassword]", "DifferentPassword!")
    # Client-side JS should show mismatch error and disable button
    expect(page.locator("#matchError")).to_be_visible()
    expect(page.locator("#submitBtn")).to_be_disabled()


# ─────────────────────────────────────────────────────────────
# T10 — Login page has 'Forgot your password?' link
# ─────────────────────────────────────────────────────────────

def test_login_page_has_forgot_password_link(page: Page):
    page.goto("http://localhost:8080/login")
    link = page.locator("a[href='/forgot-password']")
    expect(link).to_be_visible()
    expect(link).to_contain_text("Forgot")


# ─────────────────────────────────────────────────────────────
# T11 — Login page shows reset success banner after reset
# ─────────────────────────────────────────────────────────────

def test_login_shows_reset_success_banner(page: Page):
    page.goto("http://localhost:8080/login?reset=success")
    expect(page.locator("text=Password reset successfully")).to_be_visible()
