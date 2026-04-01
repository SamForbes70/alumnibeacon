"""P6 Billing — Playwright tests."""
import pytest
import os
import subprocess
from playwright.sync_api import Page, expect


# ── Pricing page (public) ─────────────────────────────────────────────────────

class TestPricingPage:
    """Public /pricing page — no auth required."""

    def test_pricing_page_loads(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page).to_have_title("Pricing | AlumniBeacon")

    def test_pricing_has_three_plans(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page.locator("text=Starter").first).to_be_visible()
        expect(page.locator("text=Professional").first).to_be_visible()
        expect(page.locator("text=Enterprise").first).to_be_visible()

    def test_pricing_shows_prices(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page.locator("#price-starter")).to_have_text("$49")
        expect(page.locator("#price-professional")).to_have_text("$199")

    def test_pricing_annual_toggle(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        # Default: monthly prices
        expect(page.locator("#price-starter")).to_have_text("$49")
        # Click toggle
        page.locator("#billing-toggle").click()
        # Annual prices should show
        expect(page.locator("#price-starter")).to_have_text("$39")
        expect(page.locator("#price-professional")).to_have_text("$159")
        # Annual note appears
        expect(page.locator("#annual-note")).to_be_visible()

    def test_pricing_toggle_back_to_monthly(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        page.locator("#billing-toggle").click()  # annual
        page.locator("#billing-toggle").click()  # back to monthly
        expect(page.locator("#price-starter")).to_have_text("$49")
        expect(page.locator("#annual-note")).to_be_hidden()

    def test_pricing_get_started_links(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        links = page.locator("a[href*='checkout']").all()
        assert len(links) >= 2, "Expected at least 2 checkout links"

    def test_pricing_enterprise_contact_sales(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page.locator("text=Contact Sales").first).to_be_visible()

    def test_pricing_most_popular_badge(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page.locator("text=Most Popular")).to_be_visible()

    def test_pricing_nav_has_signin(self, page: Page):
        page.goto("http://localhost:8080/pricing")
        expect(page.get_by_role("link", name="Sign in")).to_be_visible()

    def test_pricing_accessible_without_auth(self, page: Page):
        """Pricing page must not redirect to login."""
        page.goto("http://localhost:8080/pricing")
        assert "/login" not in page.url, "Pricing page should not require auth"


# ── Billing dashboard (authenticated) ────────────────────────────────────────

class TestBillingDashboard:
    """Authenticated /billing dashboard."""

    def test_billing_redirects_unauthenticated(self, page: Page):
        page.goto("http://localhost:8080/billing")
        assert "/login" in page.url, "Billing should redirect to login"

    def test_billing_dashboard_loads(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        expect(auth_page.get_by_role("heading", name="Billing & Subscription")).to_be_visible()

    def test_billing_shows_current_plan(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        # Should show one of the plan names
        body = auth_page.locator("body").inner_text()
        assert any(p in body for p in ["Starter", "Professional", "Enterprise"]),             "Billing page should show current plan"

    def test_billing_shows_usage_bar(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        expect(auth_page.locator("text=Monthly Usage")).to_be_visible()

    def test_billing_shows_subscription_status(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        body = auth_page.locator("body").inner_text()
        assert any(s in body for s in ["Active", "Trial", "Cancelled", "Past Due"]),             "Billing page should show subscription status"

    def test_billing_shows_dev_notice_when_stripe_not_configured(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        # In dev mode (no Stripe key), DEV MODE notice should appear
        expect(auth_page.locator("#stripe-dev-notice")).to_be_visible()

    def test_billing_shows_upgrade_section(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing")
        body = auth_page.locator("body").inner_text()
        # Should show upgrade options or current plan indicator
        assert "Plan" in body, "Billing page should mention Plan"

    def test_billing_success_banner(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing?success=true")
        expect(auth_page.locator("text=subscription has been activated")).to_be_visible()

    def test_billing_cancelled_banner(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing?cancelled=true")
        expect(auth_page.locator("text=Checkout cancelled")).to_be_visible()

    def test_billing_mock_banner(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing?mock=true")
        expect(auth_page.locator("text=Stripe not configured — this is a demo redirect.")).to_be_visible()


# ── Checkout redirect (no Stripe key = mock redirect) ─────────────────────────

class TestCheckoutFlow:
    """Checkout redirects — in dev mode redirects to /billing?mock."""

    def test_checkout_starter_redirects(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing/checkout/starter")
        # Without Stripe key, should redirect to /billing?mock=true&plan=starter
        assert "billing" in auth_page.url, "Checkout should redirect to billing page"

    def test_checkout_professional_redirects(self, auth_page: Page):
        auth_page.goto("http://localhost:8080/billing/checkout/professional")
        assert "billing" in auth_page.url, "Checkout should redirect to billing page"

    def test_checkout_requires_auth(self, page: Page):
        page.goto("http://localhost:8080/billing/checkout/starter")
        assert "/login" in page.url, "Checkout should require auth"


# ── Webhook endpoint (public) ─────────────────────────────────────────────────

class TestWebhookEndpoint:
    """POST /billing/webhook — public endpoint."""

    def test_webhook_accepts_post(self):
        import urllib.request
        import urllib.error
        req = urllib.request.Request(
            "http://localhost:8080/billing/webhook",
            data=b'{"type":"test"}',
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        try:
            resp = urllib.request.urlopen(req)
            assert resp.status in [200, 400], f"Webhook should return 200 or 400, got {resp.status}"
        except urllib.error.HTTPError as e:
            # 400 is acceptable (invalid signature) — proves endpoint is reachable
            assert e.code == 400, f"Expected 400 for invalid webhook, got {e.code}"


# ── Backend file structure checks ─────────────────────────────────────────────

class TestBillingBackendFiles:
    """Verify all P6 backend files exist and have correct content."""

    def test_stripe_config_exists(self):
        path = "src/main/java/com/alumnibeacon/config/StripeConfig.java"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "stripe.secret-key" in content
        assert "isEnabled" in content

    def test_stripe_service_exists(self):
        path = "src/main/java/com/alumnibeacon/service/StripeService.java"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "createCheckoutSession" in content
        assert "createPortalSession" in content
        assert "handleWebhook" in content
        assert "checkout.session.completed" in content

    def test_billing_controller_exists(self):
        path = "src/main/java/com/alumnibeacon/controller/BillingController.java"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "/pricing" in content
        assert "/billing/webhook" in content
        assert "TenantDetails" in content

    def test_v5_migration_exists(self):
        path = "src/main/resources/db/migration/V5__add_billing.sql"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "subscription_status" in content
        assert "monthly_investigation_limit" in content

    def test_pricing_template_exists(self):
        path = "src/main/resources/templates/billing/pricing.html"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "price-starter" in content
        assert "price-professional" in content
        assert "billing-toggle" in content

    def test_dashboard_template_exists(self):
        path = "src/main/resources/templates/billing/dashboard.html"
        assert os.path.exists(path), f"Missing: {path}"
        content = open(path).read()
        assert "stripe-dev-notice" in content
        assert "usagePercent" in content

    def test_tenant_has_billing_fields(self):
        path = "src/main/java/com/alumnibeacon/model/Tenant.java"
        content = open(path).read()
        assert "subscriptionStatus" in content
        assert "monthlyInvestigationLimit" in content
        assert "investigationsUsedThisMonth" in content

    def test_stripe_config_in_properties(self):
        content = open("src/main/resources/application.properties").read()
        assert "stripe.secret-key" in content
        assert "stripe.webhook-secret" in content
