"""
Admin Panel Tests — AlumniBeacon

Covers: admin page loads, settings form present, sidebar navigation,
        non-admin access control.
"""
import pytest
from conftest import BASE_URL


class TestAdminPanel:

    def test_admin_page_loads(self, shared_auth_page):
        """Admin page renders without error for ADMIN role."""
        shared_auth_page.goto(f"{BASE_URL}/admin")
        content = shared_auth_page.content()
        assert "whitelabel error" not in content.lower()
        assert "stacktrace"       not in content.lower()
        assert "403"              not in shared_auth_page.title()

    def test_admin_has_settings_form(self, shared_auth_page):
        """Admin page contains a settings form."""
        shared_auth_page.goto(f"{BASE_URL}/admin")
        content = shared_auth_page.content()
        # Settings form should be present
        assert "<form" in content
        assert any(word in content.lower() for word in
                   ["settings", "organisation", "organization", "tenant", "plan"])

    def test_admin_has_sidebar(self, shared_auth_page):
        """Admin page has sidebar navigation."""
        shared_auth_page.goto(f"{BASE_URL}/admin")
        content = shared_auth_page.content()
        assert "/dashboard"      in content
        assert "/investigations" in content

    def test_admin_no_csrf_errors(self, shared_auth_page):
        """Admin page renders without CSRF-related template errors."""
        shared_auth_page.goto(f"{BASE_URL}/admin")
        content = shared_auth_page.content()
        # Should not contain Thymeleaf error about _csrf being null
        assert "_csrf" not in content or "null" not in content
        assert "TemplateProcessingException" not in content
        assert "Exception evaluating" not in content

    def test_admin_credits_page_loads(self, shared_auth_page):
        """Admin credits page renders without error."""
        shared_auth_page.goto(f"{BASE_URL}/admin/credits")
        content = shared_auth_page.content()
        assert "whitelabel error" not in content.lower()
        assert "stacktrace"       not in content.lower()

    def test_admin_unauthenticated_redirects(self, page):
        """Unauthenticated access to /admin redirects to login."""
        page.goto(f"{BASE_URL}/admin")
        page.wait_for_timeout(2000)
        assert "/login" in page.url
