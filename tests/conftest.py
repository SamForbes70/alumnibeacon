"""
AlumniBeacon Playwright Test Configuration

Shared fixtures for all test modules.
App must be running on http://localhost:8080 before running tests.
"""
import pytest
from playwright.sync_api import sync_playwright

BASE_URL    = "http://localhost:8080"
ADMIN_EMAIL = "admin@utas.edu.au"
ADMIN_PASS  = "password123"

LAUNCH_ARGS = ["--no-sandbox", "--disable-dev-shm-usage"]


@pytest.fixture(scope="session")
def browser():
    """Single Chromium instance for the entire test session."""
    with sync_playwright() as p:
        b = p.chromium.launch(headless=True, args=LAUNCH_ARGS)
        yield b
        b.close()


@pytest.fixture(scope="function")
def page(browser):
    """Fresh isolated context + page per test (no shared cookies)."""
    ctx  = browser.new_context(base_url=BASE_URL)
    page = ctx.new_page()
    yield page
    ctx.close()


@pytest.fixture(scope="function")
def auth_page(browser):
    """
    Authenticated page — logs in as admin before yielding.
    Fresh context per test for full isolation.
    """
    ctx  = browser.new_context(base_url=BASE_URL)
    page = ctx.new_page()
    _login(page)
    yield page
    ctx.close()


@pytest.fixture(scope="module")
def shared_auth_page(browser):
    """
    Module-scoped authenticated page — shared across all tests in a module.
    Faster than per-function login; use when tests don't mutate auth state.
    """
    ctx  = browser.new_context(base_url=BASE_URL)
    page = ctx.new_page()
    _login(page)
    yield page
    ctx.close()


def _login(page):
    """Perform login and wait for dashboard redirect."""
    page.goto(f"{BASE_URL}/login")
    page.fill('input[name="email"]',    ADMIN_EMAIL)
    page.fill('input[name="password"]', ADMIN_PASS)
    page.click('button[type="submit"]')
    page.wait_for_url(f"{BASE_URL}/dashboard", timeout=15_000)
