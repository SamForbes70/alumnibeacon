"""
Investigation List, Filter, Pagination & Detail Tests — AlumniBeacon

Covers: list loads, search filter, status filter, combined filter,
        empty state, detail page, new investigation form.
"""
import pytest
from conftest import BASE_URL


class TestInvestigationList:

    def test_list_loads(self, shared_auth_page):
        """Investigation list renders without error."""
        shared_auth_page.goto(f"{BASE_URL}/investigations")
        assert shared_auth_page.url == f"{BASE_URL}/investigations"
        content = shared_auth_page.content()
        assert "whitelabel error" not in content.lower()
        assert "stacktrace"       not in content.lower()

    def test_list_shows_seeded_investigations(self, shared_auth_page):
        """List shows at least the 8 seeded investigations."""
        shared_auth_page.goto(f"{BASE_URL}/investigations")
        content = shared_auth_page.content()
        # Seeded names should appear
        assert "Mitchell"  in content
        assert "Thompson"  in content
        assert "Chen"      in content
        assert "Williams"  in content

    def test_list_has_new_investigation_button(self, shared_auth_page):
        """List page has a link/button to create a new investigation."""
        shared_auth_page.goto(f"{BASE_URL}/investigations")
        content = shared_auth_page.content()
        assert "/investigations/new" in content

    def test_list_shows_status_badges(self, shared_auth_page):
        """Status badges (COMPLETED, PENDING, etc.) are visible."""
        shared_auth_page.goto(f"{BASE_URL}/investigations")
        content = shared_auth_page.content()
        # At least one status should appear
        # Template renders human-readable labels (Complete/Queued/Processing/Failed)
        # Also check data-status attribute which holds raw enum value
        assert any(s in content for s in [
            "Complete", "Queued", "Processing", "Failed",
            "COMPLETED", "PENDING", "PROCESSING", "FAILED"
        ])


class TestInvestigationSearch:

    def test_search_by_name_filters_results(self, shared_auth_page):
        """Searching 'mitchell' returns only Mitchell investigations."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?search=mitchell")
        content = shared_auth_page.content()
        assert "Mitchell" in content
        # Non-Mitchell names should not appear
        assert "Thompson" not in content
        assert "Chen"     not in content

    def test_search_no_match_shows_empty_state(self, shared_auth_page):
        """Searching for a non-existent name shows empty state message."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?search=zzznomatch999")
        content = shared_auth_page.content()
        # Should show empty state — no results
        assert "Mitchell" not in content
        assert "Thompson" not in content
        # Should show a 'no results' or 'clear filters' message
        assert any(phrase in content.lower() for phrase in
                   ["no investigation", "no results", "clear", "found", "empty"])

    def test_search_preserves_query_in_input(self, shared_auth_page):
        """Search input retains the query value after filtering."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?search=mitchell")
        content = shared_auth_page.content()
        assert "mitchell" in content.lower()


class TestInvestigationStatusFilter:

    def test_filter_completed_shows_only_completed(self, shared_auth_page):
        """Status filter COMPLETED shows only completed investigations."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?status=COMPLETED")
        content = shared_auth_page.content()
        assert "COMPLETED" in content
        # PENDING and FAILED should not appear as status badges
        # PENDING appears in dropdown - check badge text
        assert ">PENDING<" not in content
        assert ">PROCESSING<" not in content

    def test_filter_pending_shows_only_pending(self, shared_auth_page):
        """Status filter PENDING shows only pending investigations."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?status=PENDING")
        content = shared_auth_page.content()
        assert "Michael Thompson" in content
        assert ">COMPLETED<" not in content

    def test_filter_failed_shows_only_failed(self, shared_auth_page):
        """Status filter FAILED shows only failed investigations."""
        shared_auth_page.goto(f"{BASE_URL}/investigations?status=FAILED")
        content = shared_auth_page.content()
        assert "Jessica Williams" in content
        assert ">COMPLETED<" not in content


class TestInvestigationCombinedFilter:

    def test_search_and_status_combined(self, shared_auth_page):
        """Combined search + status filter returns correct subset."""
        shared_auth_page.goto(
            f"{BASE_URL}/investigations?search=mitchell&status=COMPLETED")
        content = shared_auth_page.content()
        # Both Mitchells are COMPLETED — both should appear
        assert "Mitchell" in content
        # Non-Mitchell names should not appear
        assert "Thompson" not in content
        assert "Noah"     not in content

    def test_combined_filter_no_match(self, shared_auth_page):
        """Combined filter with no match shows empty state."""
        shared_auth_page.goto(
            f"{BASE_URL}/investigations?search=mitchell&status=PENDING")
        content = shared_auth_page.content()
        # No Mitchell is PENDING
        assert "Tom Mitchell" not in content
        assert "David Mitchell" not in content


class TestInvestigationDetail:

    def _get_first_investigation_id(self, page):
        """Helper: get the ID of the first investigation from the list."""
        page.goto(f"{BASE_URL}/investigations")
        # Find first investigation link
        links = page.locator('a[href*="/investigations/"]').all()
        for link in links:
            href = link.get_attribute("href")
            if href and "/investigations/" in href and "/new" not in href:
                return href.split("/investigations/")[1].split("?")[0]
        return None

    def test_detail_page_loads(self, shared_auth_page):
        """Investigation detail page loads without error."""
        inv_id = self._get_first_investigation_id(shared_auth_page)
        assert inv_id is not None, "No investigation found in list"

        shared_auth_page.goto(f"{BASE_URL}/investigations/{inv_id}")
        content = shared_auth_page.content()
        assert "whitelabel error" not in content.lower()
        assert "stacktrace"       not in content.lower()

    def test_detail_shows_subject_name(self, shared_auth_page):
        """Detail page shows the subject's name."""
        inv_id = self._get_first_investigation_id(shared_auth_page)
        assert inv_id is not None

        shared_auth_page.goto(f"{BASE_URL}/investigations/{inv_id}")
        content = shared_auth_page.content()
        # At least one seeded name should appear
        assert any(name in content for name in
                   ["Mitchell", "Thompson", "Chen", "Nguyen", "Williams", "Brown", "Taylor",
                    "Green", "Parker", "Davis", "Wilson", "Johnson", "Smith", "Jones"])

    def test_detail_shows_status(self, shared_auth_page):
        """Detail page shows investigation status."""
        inv_id = self._get_first_investigation_id(shared_auth_page)
        assert inv_id is not None

        shared_auth_page.goto(f"{BASE_URL}/investigations/{inv_id}")
        content = shared_auth_page.content()
        # Template renders human-readable labels (Complete/Queued/Processing/Failed)
        # Also check data-status attribute which holds raw enum value
        assert any(s in content for s in [
            "Complete", "Queued", "Processing", "Failed",
            "COMPLETED", "PENDING", "PROCESSING", "FAILED"
        ])


class TestNewInvestigationForm:

    def test_new_investigation_form_loads(self, shared_auth_page):
        """New investigation form renders with required fields."""
        shared_auth_page.goto(f"{BASE_URL}/investigations/new")
        content = shared_auth_page.content()
        assert "whitelabel error" not in content.lower()
        # Form should have subject name field
        assert any(field in content for field in
                   ["subjectName", "subject_name", "name", "Subject"])

    def test_new_investigation_has_submit_button(self, shared_auth_page):
        """New investigation form has a submit button."""
        shared_auth_page.goto(f"{BASE_URL}/investigations/new")
        assert shared_auth_page.locator('button[type="submit"], input[type="submit"]').count() > 0
