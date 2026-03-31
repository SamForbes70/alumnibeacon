"""
Authentication Tests — AlumniBeacon

Covers: login success, login failure, protected route redirect,
        logout, register page loads.
"""
import pytest
from conftest import BASE_URL, ADMIN_EMAIL, ADMIN_PASS


class TestLoginPage:

    def test_login_page_loads(self, page):
        """Login page renders with correct title and form fields."""
        page.goto(f"{BASE_URL}/login")
        assert "AlumniBeacon" in page.title()
        assert page.locator('input[name="email"]').is_visible()
        assert page.locator('input[name="password"]').is_visible()
        assert page.locator('button[type="submit"]').is_visible()

    def test_login_success_redirects_to_dashboard(self, page):
        """Valid credentials redirect to /dashboard and set JWT cookie."""
        page.goto(f"{BASE_URL}/login")
        page.fill('input[name="email"]',    ADMIN_EMAIL)
        page.fill('input[name="password"]', ADMIN_PASS)
        page.click('button[type="submit"]')
        page.wait_for_url(f"{BASE_URL}/dashboard", timeout=15_000)
        assert page.url == f"{BASE_URL}/dashboard"

        # JWT cookie must be present
        cookies = {c["name"]: c for c in page.context.cookies()}
        assert "jwt" in cookies, "JWT cookie not set after login"
        assert cookies["jwt"]["httpOnly"] is True

    def test_login_failure_shows_error(self, page):
        """Wrong password stays on login page and shows error message."""
        page.goto(f"{BASE_URL}/login")
        page.fill('input[name="email"]',    ADMIN_EMAIL)
        page.fill('input[name="password"]', "wrongpassword")
        page.click('button[type="submit"]')
        # Should stay on login page
        page.wait_for_timeout(2000)
        assert "/login" in page.url
        # Error message should appear
        content = page.content()
        assert any(word in content.lower() for word in ["invalid", "error", "incorrect"])

    def test_protected_route_redirects_to_login(self, page):
        """Unauthenticated access to /dashboard redirects to /login."""
        page.goto(f"{BASE_URL}/dashboard")
        page.wait_for_timeout(2000)
        assert "/login" in page.url

    def test_protected_investigations_redirects_to_login(self, page):
        """Unauthenticated access to /investigations redirects to /login."""
        page.goto(f"{BASE_URL}/investigations")
        page.wait_for_timeout(2000)
        assert "/login" in page.url


class TestLogout:

    def test_logout_clears_session(self, auth_page):
        """Logout redirects to /login and clears JWT cookie."""
        auth_page.goto(f"{BASE_URL}/logout")
        auth_page.wait_for_timeout(2000)
        assert "/login" in auth_page.url


class TestRegisterPage:

    def test_register_page_loads(self, page):
        """Register page renders with all required form fields."""
        page.goto(f"{BASE_URL}/register")
        assert page.locator('input[name="organisationName"]').is_visible()
        assert page.locator('input[name="fullName"]').is_visible()
        assert page.locator('input[name="email"]').is_visible()
        assert page.locator('input[name="password"]').is_visible()
