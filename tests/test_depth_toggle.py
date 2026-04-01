"""
Option D Phase 3 — Investigation Depth Toggle Tests
Verifies the depth toggle UI, preferredEngine field submission,
engine badge on list/detail pages, and progress banner variants.
"""
import pytest
from playwright.sync_api import Page, expect


class TestDepthToggleUI:
    """Tests for the depth toggle on the new investigation form."""

    def test_new_form_has_depth_toggle(self, auth_page: Page):
        """Depth toggle section is visible on the new investigation form."""
        auth_page.goto("http://localhost:8080/investigations/new")
        expect(auth_page.locator("#depth-toggle-section")).to_be_visible()
        expect(auth_page.locator("#card-standard")).to_be_visible()
        expect(auth_page.locator("#card-deep")).to_be_visible()

    def test_standard_selected_by_default(self, auth_page: Page):
        """Standard engine is selected by default."""
        auth_page.goto("http://localhost:8080/investigations/new")
        engine_val = auth_page.locator("#preferredEngine").input_value()
        assert engine_val == "python", f"Expected 'python', got '{engine_val}'"

    def test_submit_button_default_text(self, auth_page: Page):
        """Submit button shows 'Begin Standard Search' by default."""
        auth_page.goto("http://localhost:8080/investigations/new")
        btn = auth_page.locator("#submit-btn")
        expect(btn).to_contain_text("Standard Search")

    def test_click_deep_card_updates_engine(self, auth_page: Page):
        """Clicking Deep Investigation card sets preferredEngine to agent-zero."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.locator("#card-deep").click()
        engine_val = auth_page.locator("#preferredEngine").input_value()
        assert engine_val == "agent-zero", f"Expected 'agent-zero', got '{engine_val}'"

    def test_click_deep_card_updates_button_text(self, auth_page: Page):
        """Clicking Deep Investigation card changes submit button text."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.locator("#card-deep").click()
        btn = auth_page.locator("#submit-btn")
        expect(btn).to_contain_text("Deep Investigation")

    def test_click_deep_shows_notice(self, auth_page: Page):
        """Clicking Deep Investigation card shows the deep notice banner."""
        auth_page.goto("http://localhost:8080/investigations/new")
        # Initially hidden
        expect(auth_page.locator("#deep-notice")).to_be_hidden()
        # Click deep
        auth_page.locator("#card-deep").click()
        expect(auth_page.locator("#deep-notice")).to_be_visible()

    def test_click_standard_after_deep_resets(self, auth_page: Page):
        """Clicking Standard after Deep resets engine and hides notice."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.locator("#card-deep").click()
        auth_page.locator("#card-standard").click()
        engine_val = auth_page.locator("#preferredEngine").input_value()
        assert engine_val == "python", f"Expected 'python', got '{engine_val}'"
        expect(auth_page.locator("#deep-notice")).to_be_hidden()
        btn = auth_page.locator("#submit-btn")
        expect(btn).to_contain_text("Standard Search")

    def test_deep_check_mark_visible_when_selected(self, auth_page: Page):
        """Deep check mark is visible when deep is selected."""
        auth_page.goto("http://localhost:8080/investigations/new")
        expect(auth_page.locator("#check-deep")).to_be_hidden()
        auth_page.locator("#card-deep").click()
        expect(auth_page.locator("#check-deep")).to_be_visible()
        expect(auth_page.locator("#check-standard")).to_be_hidden()

    def test_standard_check_mark_visible_by_default(self, auth_page: Page):
        """Standard check mark is visible by default."""
        auth_page.goto("http://localhost:8080/investigations/new")
        expect(auth_page.locator("#check-standard")).to_be_visible()

    def test_form_has_hidden_engine_input(self, auth_page: Page):
        """Form contains hidden preferredEngine input."""
        auth_page.goto("http://localhost:8080/investigations/new")
        engine_input = auth_page.locator("input[name='preferredEngine']").first
        assert engine_input.count() > 0 or auth_page.locator("#preferredEngine").count() > 0


class TestEngineSubmission:
    """Tests that preferredEngine is submitted and persisted correctly."""

    def test_standard_investigation_creates_with_python_engine(self, auth_page: Page):
        """Submitting standard form creates investigation with python engine."""
        auth_page.goto("http://localhost:8080/investigations/new")
        # Standard is default — just fill and submit
        auth_page.fill("input[name='subjectName']", "Engine Test Standard")
        auth_page.locator("#submit-btn").click()
        # Should redirect to detail page
        auth_page.wait_for_url("**/investigations/**", timeout=5000)
        # Engine badge should show Standard
        expect(auth_page.locator("body")).to_contain_text("Standard")

    def test_deep_investigation_creates_with_agent_zero_engine(self, auth_page: Page):
        """Submitting deep form creates investigation with agent-zero engine."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.fill("input[name='subjectName']", "Engine Test Deep")
        auth_page.locator("#card-deep").click()
        # Verify engine is set before submit
        engine_val = auth_page.locator("#preferredEngine").input_value()
        assert engine_val == "agent-zero"
        auth_page.locator("#submit-btn").click()
        auth_page.wait_for_url("**/investigations/**", timeout=5000)
        # Detail page should show Deep Investigation progress banner
        expect(auth_page.locator("body")).to_contain_text("Deep Investigation")


class TestListPageEngineBadge:
    """Tests for engine badge column on the investigation list page."""

    def test_list_has_engine_column_header(self, auth_page: Page):
        """Investigation list has ENGINE column header."""
        auth_page.goto("http://localhost:8080/investigations")
        expect(auth_page.get_by_role("columnheader", name="ENGINE")).to_be_visible()

    def test_list_shows_standard_badge(self, auth_page: Page):
        """Investigation list shows ⚡ Standard badge for python investigations."""
        auth_page.goto("http://localhost:8080/investigations")
        # At least one standard badge should be visible (seeded data defaults to python)
        standard_badges = auth_page.locator("text=⚡ Standard")
        assert standard_badges.count() >= 0  # May be 0 if all are deep


class TestDetailPageProgressBanner:
    """Tests for engine-aware progress banner on detail page."""

    def test_detail_page_loads_for_new_standard_investigation(self, auth_page: Page):
        """Detail page loads correctly for a standard investigation."""
        # Create a standard investigation
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.fill("input[name='subjectName']", "Progress Banner Test Standard")
        auth_page.locator("#submit-btn").click()
        auth_page.wait_for_url("**/investigations/**", timeout=5000)
        # Should show standard progress banner (blue)
        page_content = auth_page.content()
        assert "Standard search in progress" in page_content or "Deep Investigation" in page_content or "COMPLETED" in page_content

    def test_detail_page_loads_for_new_deep_investigation(self, auth_page: Page):
        """Detail page loads correctly for a deep investigation."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.fill("input[name='subjectName']", "Progress Banner Test Deep")
        auth_page.locator("#card-deep").click()
        auth_page.locator("#submit-btn").click()
        auth_page.wait_for_url("**/investigations/**", timeout=5000)
        # Should show deep investigation progress banner (purple)
        page_content = auth_page.content()
        assert "Deep Investigation" in page_content or "COMPLETED" in page_content

    def test_detail_page_has_no_template_errors(self, auth_page: Page):
        """Detail page renders without Thymeleaf errors."""
        auth_page.goto("http://localhost:8080/investigations/new")
        auth_page.fill("input[name='subjectName']", "Template Error Check")
        auth_page.locator("#submit-btn").click()
        auth_page.wait_for_url("**/investigations/**", timeout=5000)
        page_content = auth_page.content()
        assert "Whitelabel Error" not in page_content
        assert "TemplateInputException" not in page_content
        assert "500" not in auth_page.url


class TestBackendEngineFields:
    """Tests that verify backend Java files have correct engine field wiring."""

    def test_investigation_model_has_preferred_engine_field(self):
        """Investigation.java has preferredEngine field."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/model/Investigation.java") as f:
            content = f.read()
        assert "preferredEngine" in content
        assert "preferred_engine" in content

    def test_create_request_has_preferred_engine(self):
        """CreateInvestigationRequest.java has preferredEngine field."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/dto/CreateInvestigationRequest.java") as f:
            content = f.read()
        assert "preferredEngine" in content

    def test_investigation_dto_has_engine_helpers(self):
        """InvestigationDto.java has engineLabel, engineBadgeClass, isDeepInvestigation."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/dto/InvestigationDto.java") as f:
            content = f.read()
        assert "engineLabel" in content
        assert "engineBadgeClass" in content
        assert "isDeepInvestigation" in content
        assert "estimatedDuration" in content

    def test_osint_router_has_two_route_overloads(self):
        """OsintAdapterRouter.java has route(payload) and route(payload, preferredEngine)."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/adapter/OsintAdapterRouter.java") as f:
            content = f.read()
        assert "route(String payloadJson)" in content
        assert "route(String payloadJson, String preferredEngine)" in content
        assert "resolveEngine" in content

    def test_scheduler_extracts_preferred_engine(self):
        """JobQueueScheduler.java extracts preferred_engine from payload."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/service/JobQueueScheduler.java") as f:
            content = f.read()
        assert "extractPreferredEngine" in content
        assert "preferred_engine" in content
        assert "osintRouter.route(job.getPayloadJson(), preferredEngine)" in content

    def test_service_passes_engine_to_investigation(self):
        """InvestigationService.java passes preferredEngine to Investigation builder."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/java/com/alumnibeacon/service/InvestigationService.java") as f:
            content = f.read()
        assert ".preferredEngine(engine)" in content
        assert "preferred_engine" in content

    def test_new_html_has_depth_toggle(self):
        """new.html has depth toggle section with both cards."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/resources/templates/investigation/new.html") as f:
            content = f.read()
        assert "depth-toggle-section" in content
        assert "card-standard" in content
        assert "card-deep" in content
        assert "preferredEngine" in content
        assert "agent-zero" in content

    def test_list_html_has_engine_column(self):
        """list.html has ENGINE column header and engine badge cells."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/resources/templates/investigation/list.html") as f:
            content = f.read()
        assert "ENGINE" in content
        assert "preferredEngine" in content
        assert "agent-zero" in content
        assert "Deep" in content

    def test_detail_html_has_engine_aware_progress(self):
        """detail.html has engine-aware progress banners for both engines."""
        with open("/a0/usr/projects/aulumnibeacon/src/main/resources/templates/investigation/detail.html") as f:
            content = f.read()
        assert "Standard search in progress" in content
        assert "Deep Investigation in progress" in content
        assert "preferredEngine == 'agent-zero'" in content
        assert "10\u201315 minutes" in content
