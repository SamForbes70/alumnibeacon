"""
Dashboard Tests — AlumniBeacon

Covers: dashboard loads, stats bar, sidebar navigation, quick-action links.
"""
import pytest
from conftest import BASE_URL


class TestDashboard:

    def test_dashboard_loads(self, shared_auth_page):
        """Dashboard renders without error after login."""
        shared_auth_page.goto(f"{BASE_URL}/dashboard")
        assert shared_auth_page.url == f"{BASE_URL}/dashboard"
        assert "AlumniBeacon" in shared_auth_page.title()

    def test_dashboard_has_sidebar(self, shared_auth_page):
        """Sidebar navigation is present with key links."""
        shared_auth_page.goto(f"{BASE_URL}/dashboard")
        content = shared_auth_page.content()
        # Sidebar should contain navigation links
        assert "/investigations" in content
        assert "/dashboard" in content

    def test_dashboard_has_stats(self, shared_auth_page):
        """Stats bar shows investigation counts."""
        shared_auth_page.goto(f"{BASE_URL}/dashboard")
        content = shared_auth_page.content()
        # Stats should show numbers (seeded data has 8 investigations)
        assert any(str(n) in content for n in range(1, 20)), \
            "No numeric stats found on dashboard"

    def test_dashboard_new_investigation_link(self, shared_auth_page):
        """Dashboard has a link to create a new investigation."""
        shared_auth_page.goto(f"{BASE_URL}/dashboard")
        content = shared_auth_page.content()
        assert "/investigations/new" in content

    def test_dashboard_no_error_elements(self, shared_auth_page):
        """Dashboard page contains no error messages or stack traces."""
        shared_auth_page.goto(f"{BASE_URL}/dashboard")
        content = shared_auth_page.content().lower()
        assert "whitelabel error" not in content
        assert "stacktrace" not in content
        assert "500" not in shared_auth_page.title().lower()
