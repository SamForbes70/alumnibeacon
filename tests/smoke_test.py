#!/usr/bin/env python3
"""
Standalone smoke test — verifies Playwright can login and access AlumniBeacon.
Run: python3 tests/smoke_test.py
"""
import sys
import time
from playwright.sync_api import sync_playwright

BASE_URL = "http://localhost:8080"
EMAIL = "admin@utas.edu.au"
PASSWORD = "password123"

def run():
    results = []

    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-web-security",
            ]
        )
        context = browser.new_context(
            ignore_https_errors=True,
            # Treat localhost as secure context so secure cookies work
            base_url=BASE_URL,
        )
        page = context.new_page()
        page.set_default_timeout(15000)

        # --- Test 1: Login page loads ---
        try:
            page.goto(f"{BASE_URL}/login")
            assert "login" in page.url.lower() or page.locator('input[name="email"]').count() > 0
            results.append(("T1 Login page loads", "PASS", ""))
        except Exception as e:
            results.append(("T1 Login page loads", "FAIL", str(e)))

        # --- Test 2: Login with valid credentials ---
        try:
            page.goto(f"{BASE_URL}/login")
            page.fill('input[name="email"]', EMAIL)
            page.fill('input[name="password"]', PASSWORD)
            page.click('button[type="submit"]')
            page.wait_for_timeout(3000)
            url_after = page.url
            # Should redirect away from login
            assert "/login" not in url_after or "error" not in url_after, \
                f"Still on login page: {url_after}"
            results.append(("T2 Login succeeds", "PASS", f"→ {url_after}"))
        except Exception as e:
            results.append(("T2 Login succeeds", "FAIL", str(e)))
            # If login fails, inject JWT cookie directly as fallback
            print("  Trying direct JWT cookie injection...")
            import subprocess, json
            r = subprocess.run(
                ['curl', '-s', '-c', '-', '-X', 'POST',
                 f'{BASE_URL}/auth/login',
                 '-d', f'email={EMAIL}&password={PASSWORD}'],
                capture_output=True, text=True
            )
            for line in r.stdout.splitlines():
                if 'jwt' in line.lower():
                    parts = line.split()
                    if len(parts) >= 7:
                        token = parts[-1]
                        context.add_cookies([{
                            'name': 'jwt', 'value': token,
                            'domain': 'localhost', 'path': '/',
                            'httpOnly': True, 'secure': False
                        }])
                        print(f"  Injected JWT cookie: {token[:30]}...")
                        break

        # --- Test 3: Dashboard accessible ---
        try:
            page.goto(f"{BASE_URL}/dashboard")
            page.wait_for_timeout(2000)
            content = page.content()
            assert "whitelabel error" not in content.lower()
            assert "stacktrace" not in content.lower()
            assert "/login" not in page.url  # not redirected back to login
            results.append(("T3 Dashboard accessible", "PASS", f"URL: {page.url}"))
        except Exception as e:
            results.append(("T3 Dashboard accessible", "FAIL", str(e)))

        # --- Test 4: Investigations list accessible ---
        # --- Test 4: Investigations list accessible ---
        try:
            page.goto(f"{BASE_URL}/investigations")
            page.wait_for_timeout(2000)
            content = page.content()
            current_url = page.url
            assert "whitelabel error" not in content.lower(), f"Whitelabel error on {current_url}"
            assert "/login" not in current_url, f"Redirected to login: {current_url}"
            assert "Mitchell" in content or "Thompson" in content or "Chen" in content, \
                f"No investigation names found. URL={current_url}, content snippet: {content[500:800]}"
            results.append(("T4 Investigations list", "PASS", ""))
        except Exception as e:
            results.append(("T4 Investigations list", "FAIL", str(e)[:200]))

        # --- Test 5: Search filter works ---
        try:
            page.goto(f"{BASE_URL}/investigations?search=mitchell")
            page.wait_for_timeout(2000)
            content = page.content()
            current_url = page.url
            assert "/login" not in current_url, f"Redirected to login: {current_url}"
            assert "Mitchell" in content, f"Mitchell not found. URL={current_url}"
            assert "Thompson" not in content, "Thompson appeared in Mitchell-only search"
            results.append(("T5 Search filter", "PASS", ""))
        except Exception as e:
            results.append(("T5 Search filter", "FAIL", str(e)[:200]))

        # --- Test 6: Status filter works ---
        try:
            page.goto(f"{BASE_URL}/investigations?status=COMPLETED")
            page.wait_for_timeout(2000)
            content = page.content()
            current_url = page.url
            assert "/login" not in current_url, f"Redirected to login: {current_url}"
            assert "COMPLETED" in content, f"COMPLETED not found. URL={current_url}"
            # PENDING appears in dropdown options - check badge text instead
            assert ">PENDING<" not in content, "PENDING badge appeared in COMPLETED-only results"
            results.append(("T6 Status filter", "PASS", ""))
        except Exception as e:
            results.append(("T6 Status filter", "FAIL", str(e)[:200]))

        # --- Test 7: Admin page accessible ---
        try:
            page.goto(f"{BASE_URL}/admin")
            page.wait_for_timeout(2000)
            content = page.content()
            assert "whitelabel error" not in content.lower()
            assert "stacktrace" not in content.lower()
            results.append(("T7 Admin page", "PASS", ""))
        except Exception as e:
            results.append(("T7 Admin page", "FAIL", str(e)))

        browser.close()

    # Print results
    print("\n" + "="*60)
    print("PLAYWRIGHT SMOKE TEST RESULTS")
    print("="*60)
    passed = failed = 0
    for name, status, detail in results:
        icon = "✅" if status == "PASS" else "❌"
        detail_str = f"  ({detail})" if detail else ""
        print(f"{icon} {status}  {name}{detail_str}")
        if status == "PASS":
            passed += 1
        else:
            failed += 1
    print("-"*60)
    print(f"Total: {passed} passed, {failed} failed")
    print("="*60)
    return failed == 0

if __name__ == "__main__":
    success = run()
    sys.exit(0 if success else 1)
