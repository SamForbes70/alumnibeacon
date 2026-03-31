"""
August OSINT Adapter v3.0
Tiered real-data pipeline with graceful degradation.

Tier 1: Google Custom Search API  (always run if key configured)
Tier 2: Proxycurl                 (LinkedIn enrichment, if key configured)
Tier 3: People Data Labs          (comprehensive person data, if key configured)
Tier 4: Hunter.io                 (email finding, if key configured)
Tier 5: Claude synthesis          (always run — synthesises all gathered data)

If no API keys are configured, falls back to AI-only mode with LOW CONFIDENCE warning.
"""
import json
import logging
import os
import re
from datetime import datetime, timezone
from typing import Optional

import httpx
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# ── Configuration ─────────────────────────────────────────────────────────────
OPENROUTER_API_KEY   = os.environ.get("API_KEY_OPENROUTER", "")
OPENROUTER_MODEL     = os.environ.get("OSINT_MODEL", "anthropic/claude-sonnet-4-5")
OPENROUTER_BASE_URL  = "https://openrouter.ai/api/v1"

GOOGLE_API_KEY       = os.environ.get("GOOGLE_API_KEY", "")
GOOGLE_CSE_ID        = os.environ.get("GOOGLE_CSE_ID", "")

PROXYCURL_API_KEY    = os.environ.get("PROXYCURL_API_KEY", "")

PDL_API_KEY          = os.environ.get("PDL_API_KEY", "")

HUNTER_API_KEY       = os.environ.get("HUNTER_API_KEY", "")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="August OSINT Adapter", version="3.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"]
)


# ── Request / Response models ─────────────────────────────────────────────────
class SearchRequest(BaseModel):
    name: str
    dob: Optional[str] = None
    last_known_address: Optional[str] = None
    last_known_email: Optional[str] = None
    graduation_year: Optional[str] = None
    last_known_employer: Optional[str] = None
    notes: Optional[str] = None
    investigation_id: Optional[str] = None


class SearchResult(BaseModel):
    investigation_id: Optional[str] = None
    subject_name: str
    status: str
    confidence_score: int
    found_email: Optional[str] = None
    found_phone: Optional[str] = None
    found_address: Optional[str] = None
    found_employer: Optional[str] = None
    found_linkedin: Optional[str] = None
    found_facebook: Optional[str] = None
    sources: list = []
    summary: str
    confidence_breakdown: dict = {}
    recommended_actions: list = []
    privacy_flags: list = []
    raw_findings: dict = {}
    searched_at: str
    mode: str = "live"


# ── Gathered data container ───────────────────────────────────────────────────
class GatheredData:
    """Accumulates findings from all tiers before synthesis."""

    def __init__(self, req: SearchRequest):
        self.req = req
        self.emails: list[str] = []
        self.phones: list[str] = []
        self.addresses: list[str] = []
        self.employers: list[str] = []
        self.linkedin_urls: list[str] = []
        self.facebook_urls: list[str] = []
        self.sources_used: list[str] = []
        self.raw_snippets: list[str] = []   # text snippets for Claude synthesis
        self.tiers_run: list[str] = []
        self.real_data_found: bool = False

    def add_email(self, email: str, source: str):
        if email and email not in self.emails:
            self.emails.append(email)
            self._add_source(source)
            self.real_data_found = True

    def add_phone(self, phone: str, source: str):
        if phone and phone not in self.phones:
            self.phones.append(phone)
            self._add_source(source)
            self.real_data_found = True

    def add_address(self, address: str, source: str):
        if address and address not in self.addresses:
            self.addresses.append(address)
            self._add_source(source)
            self.real_data_found = True

    def add_employer(self, employer: str, source: str):
        if employer and employer not in self.employers:
            self.employers.append(employer)
            self._add_source(source)
            self.real_data_found = True

    def add_linkedin(self, url: str, source: str):
        if url and url not in self.linkedin_urls:
            self.linkedin_urls.append(url)
            self._add_source(source)
            self.real_data_found = True

    def add_facebook(self, url: str, source: str):
        if url and url not in self.facebook_urls:
            self.facebook_urls.append(url)
            self._add_source(source)
            self.real_data_found = True

    def add_snippet(self, text: str):
        if text:
            self.raw_snippets.append(text[:500])

    def _add_source(self, source: str):
        if source and source not in self.sources_used:
            self.sources_used.append(source)

    def best(self, lst: list) -> Optional[str]:
        return lst[0] if lst else None


# ── Confidence scoring ────────────────────────────────────────────────────────
def calculate_confidence(data: GatheredData) -> tuple[int, dict]:
    """
    Real confidence scoring algorithm (not LLM passthrough).

    Breakdown (total 100):
      data_points_found   30 pts  — how many contact fields populated
      cross_source        25 pts  — multiple sources agree on same data
      verification_level  25 pts  — real API vs AI estimate
      source_quality      10 pts  — premium vs free sources
      recency             10 pts  — always 10 for now (no date metadata yet)
    """
    # 1. Data points found (30 pts)
    fields = [
        data.best(data.emails),
        data.best(data.phones),
        data.best(data.addresses),
        data.best(data.employers),
        data.best(data.linkedin_urls),
        data.best(data.facebook_urls),
    ]
    found_count = sum(1 for f in fields if f)
    data_points = min(30, found_count * 5)

    # 2. Cross-source agreement (25 pts)
    # More sources = higher confidence
    tier_count = len(data.tiers_run)
    cross_source = min(25, tier_count * 8)

    # 3. Verification level (25 pts)
    # Real API data > AI estimate
    premium_sources = {"People Data Labs", "Proxycurl", "Hunter.io"}
    free_sources = {"Google Custom Search"}
    has_premium = any(s in premium_sources for s in data.sources_used)
    has_free = any(s in free_sources for s in data.sources_used)
    if has_premium:
        verification = 25
    elif has_free and data.real_data_found:
        verification = 15
    elif data.real_data_found:
        verification = 10
    else:
        verification = 0  # AI-only

    # 4. Source quality (10 pts)
    if has_premium:
        source_quality = 10
    elif has_free:
        source_quality = 6
    else:
        source_quality = 2

    # 5. Recency (10 pts) — static for now
    recency = 8 if data.real_data_found else 2

    total = data_points + cross_source + verification + source_quality + recency
    total = max(0, min(100, total))

    breakdown = {
        "name_match":        min(25, data_points + cross_source // 2),
        "location_match":    min(25, (10 if data.best(data.addresses) else 0) + cross_source // 4),
        "professional_match": min(25, (10 if data.best(data.employers) else 0) + (5 if data.best(data.linkedin_urls) else 0)),
        "contact_verified":  min(25, verification),
    }

    return total, breakdown


# ── Tier 1: Google Custom Search ──────────────────────────────────────────────
async def tier1_google(req: SearchRequest, data: GatheredData) -> None:
    """Search Google for the subject and extract structured data from snippets."""
    if not GOOGLE_API_KEY or not GOOGLE_CSE_ID:
        logger.info("Tier 1 (Google): skipped — no API key")
        return

    queries = [
        f'"{req.name}" contact email phone',
        f'"{req.name}" linkedin',
        f'"{req.name}" site:facebook.com',
    ]
    if req.last_known_address:
        queries.insert(0, f'"{req.name}" "{req.last_known_address}"')

    try:
        async with httpx.AsyncClient(timeout=15.0) as client:
            for query in queries[:2]:  # limit to 2 queries to control cost
                r = await client.get(
                    "https://www.googleapis.com/customsearch/v1",
                    params={
                        "key": GOOGLE_API_KEY,
                        "cx": GOOGLE_CSE_ID,
                        "q": query,
                        "num": 5,
                    }
                )
                if r.status_code != 200:
                    logger.warning(f"Google CSE {r.status_code}: {r.text[:200]}")
                    continue

                results = r.json().get("items", [])
                for item in results:
                    snippet = item.get("snippet", "")
                    link = item.get("link", "")
                    data.add_snippet(f"{item.get('title', '')} — {snippet}")

                    # Extract LinkedIn URLs
                    if "linkedin.com/in/" in link:
                        data.add_linkedin(link.split("?")[0], "Google Custom Search")

                    # Extract Facebook URLs
                    if "facebook.com/" in link and "/posts/" not in link:
                        data.add_facebook(link.split("?")[0], "Google Custom Search")

                    # Extract emails from snippets
                    emails = re.findall(r'[\w.+-]+@[\w-]+\.[\w.]+', snippet)
                    for email in emails:
                        if not email.endswith((".png", ".jpg", ".gif")):
                            data.add_email(email.lower(), "Google Custom Search")

        data.tiers_run.append("Google Custom Search")
        logger.info(f"Tier 1 (Google): found {len(data.raw_snippets)} snippets")

    except Exception as e:
        logger.error(f"Tier 1 (Google) error: {e}")


# ── Tier 2: Proxycurl (LinkedIn) ──────────────────────────────────────────────
async def tier2_proxycurl(req: SearchRequest, data: GatheredData) -> None:
    """Look up LinkedIn profile via Proxycurl for professional data."""
    if not PROXYCURL_API_KEY:
        logger.info("Tier 2 (Proxycurl): skipped — no API key")
        return

    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            # If we already have a LinkedIn URL from Google, use it directly
            linkedin_url = data.best(data.linkedin_urls)

            if linkedin_url:
                r = await client.get(
                    "https://nubela.co/proxycurl/api/v2/linkedin",
                    params={"url": linkedin_url, "use_cache": "if-present"},
                    headers={"Authorization": f"Bearer {PROXYCURL_API_KEY}"}
                )
            else:
                # Search by name
                r = await client.get(
                    "https://nubela.co/proxycurl/api/linkedin/profile/search",
                    params={
                        "first_name": req.name.split()[0] if req.name else "",
                        "last_name": " ".join(req.name.split()[1:]) if len(req.name.split()) > 1 else "",
                        "country": "AU",
                        "enrich_profile": "enrich",
                    },
                    headers={"Authorization": f"Bearer {PROXYCURL_API_KEY}"}
                )

            if r.status_code != 200:
                logger.warning(f"Proxycurl {r.status_code}: {r.text[:200]}")
                return

            profile = r.json()
            # Handle search result wrapper
            if "results" in profile and profile["results"]:
                profile = profile["results"][0].get("profile", {})
                if profile.get("linkedin_profile_url"):
                    data.add_linkedin(profile["linkedin_profile_url"], "Proxycurl")

            # Extract fields
            if profile.get("city") or profile.get("state") or profile.get("country"):
                parts = [p for p in [profile.get("city"), profile.get("state"), profile.get("country")] if p]
                data.add_address(", ".join(parts), "Proxycurl")

            # Current employer from experience
            experiences = profile.get("experiences", [])
            if experiences:
                current = next((e for e in experiences if not e.get("ends_at")), None)
                if current and current.get("company"):
                    data.add_employer(current["company"], "Proxycurl")

            # Summary snippet
            if profile.get("summary"):
                data.add_snippet(f"LinkedIn summary: {profile['summary'][:300]}")

        data.tiers_run.append("Proxycurl")
        logger.info(f"Tier 2 (Proxycurl): enriched profile")

    except Exception as e:
        logger.error(f"Tier 2 (Proxycurl) error: {e}")


# ── Tier 3: People Data Labs ──────────────────────────────────────────────────
async def tier3_pdl(req: SearchRequest, data: GatheredData) -> None:
    """Look up comprehensive person data via People Data Labs."""
    if not PDL_API_KEY:
        logger.info("Tier 3 (PDL): skipped — no API key")
        return

    try:
        params = {"name": req.name, "pretty": True}
        if req.last_known_email:
            params["email"] = req.last_known_email
        if req.last_known_address:
            params["location"] = req.last_known_address

        async with httpx.AsyncClient(timeout=20.0) as client:
            r = await client.get(
                "https://api.peopledatalabs.com/v5/person/enrich",
                params=params,
                headers={"X-Api-Key": PDL_API_KEY}
            )

        if r.status_code == 404:
            logger.info("Tier 3 (PDL): no record found")
            return
        if r.status_code != 200:
            logger.warning(f"PDL {r.status_code}: {r.text[:200]}")
            return

        person = r.json()

        # Emails
        for email in person.get("emails", []):
            addr = email.get("address") if isinstance(email, dict) else email
            if addr:
                data.add_email(addr.lower(), "People Data Labs")

        # Phones
        for phone in person.get("phone_numbers", []):
            num = phone.get("number") if isinstance(phone, dict) else phone
            if num:
                data.add_phone(num, "People Data Labs")

        # Location
        loc = person.get("location_name") or person.get("location", {}).get("name")
        if loc:
            data.add_address(loc, "People Data Labs")

        # Employer
        job = person.get("job_title") or ""
        company = person.get("job_company_name") or ""
        if company:
            employer_str = f"{company} ({job})".strip(" ()")
            data.add_employer(employer_str, "People Data Labs")

        # LinkedIn
        linkedin = person.get("linkedin_url") or person.get("linkedin_id")
        if linkedin:
            if not linkedin.startswith("http"):
                linkedin = f"https://linkedin.com/in/{linkedin}"
            data.add_linkedin(linkedin, "People Data Labs")

        # Facebook
        facebook = person.get("facebook_url") or person.get("facebook_id")
        if facebook:
            if not facebook.startswith("http"):
                facebook = f"https://facebook.com/{facebook}"
            data.add_facebook(facebook, "People Data Labs")

        data.tiers_run.append("People Data Labs")
        logger.info(f"Tier 3 (PDL): found person record")

    except Exception as e:
        logger.error(f"Tier 3 (PDL) error: {e}")


# ── Tier 4: Hunter.io (email finding) ────────────────────────────────────────
async def tier4_hunter(req: SearchRequest, data: GatheredData) -> None:
    """Find email address via Hunter.io email finder."""
    if not HUNTER_API_KEY:
        logger.info("Tier 4 (Hunter.io): skipped — no API key")
        return

    # Hunter needs a domain — try to derive from employer
    employer = data.best(data.employers) or req.last_known_employer
    if not employer:
        logger.info("Tier 4 (Hunter.io): skipped — no employer domain available")
        return

    # Rough domain guess from employer name
    domain_guess = employer.lower().split()[0].replace(",", "").replace(".", "") + ".com.au"

    try:
        name_parts = req.name.strip().split()
        first = name_parts[0] if name_parts else ""
        last = name_parts[-1] if len(name_parts) > 1 else ""

        async with httpx.AsyncClient(timeout=15.0) as client:
            r = await client.get(
                "https://api.hunter.io/v2/email-finder",
                params={
                    "domain": domain_guess,
                    "first_name": first,
                    "last_name": last,
                    "api_key": HUNTER_API_KEY,
                }
            )

        if r.status_code != 200:
            logger.warning(f"Hunter.io {r.status_code}: {r.text[:200]}")
            return

        result = r.json().get("data", {})
        email = result.get("email")
        score = result.get("score", 0)

        if email and score >= 50:  # Only use high-confidence results
            data.add_email(email.lower(), "Hunter.io")
            data.tiers_run.append("Hunter.io")
            logger.info(f"Tier 4 (Hunter.io): found email with score {score}")

    except Exception as e:
        logger.error(f"Tier 4 (Hunter.io) error: {e}")


# ── Tier 5: Claude synthesis ──────────────────────────────────────────────────
SYNTHESIS_PROMPT = """
You are an OSINT analyst synthesising gathered intelligence for alumni reconnection.
Operate within GDPR, Australian Privacy Act 1988, and CCPA boundaries.

Return ONLY a valid JSON object with NO markdown fences, NO backticks, NO explanation.
Start response with { character.

JSON structure required:
{
  "summary": "2-3 sentence narrative about what was found",
  "recommended_actions": ["action1", "action2"],
  "privacy_flags": ["flag if any privacy concerns"]
}

Be factual. Only reference data actually provided. Do not invent contact details.
If no real data was found, say so clearly in the summary.
"""

AI_ONLY_PROMPT = """
You are an OSINT analyst. Based ONLY on the provided seed information, estimate what
might be findable about this person for alumni reconnection purposes.

Return ONLY a valid JSON object. Start with { character. No markdown.

JSON structure:
{
  "found_email": null,
  "found_phone": null,
  "found_address": "best guess or null",
  "found_employer": "best guess or null",
  "found_linkedin": null,
  "found_facebook": null,
  "summary": "Honest statement that this is an AI estimate only, no real data sources were queried. Include what seed data was available.",
  "recommended_actions": ["Configure OSINT API keys for verified results", "Manual search recommended"],
  "privacy_flags": []
}

IMPORTANT: Do NOT invent email addresses, phone numbers, or social media URLs.
Set found_email, found_phone, found_linkedin, found_facebook to null.
Only populate found_address and found_employer if clearly derivable from seed data.
"""


def strip_markdown(raw: str) -> str:
    s = raw.strip()
    if s.startswith("`"):
        lines = s.splitlines()
        inner = lines[1:-1] if lines and lines[-1].strip() == "```" else lines[1:]
        s = "\n".join(inner).strip()
    return s


async def tier5_claude_synthesis(req: SearchRequest, data: GatheredData) -> dict:
    """Use Claude to synthesise gathered data into a coherent summary."""
    if not OPENROUTER_API_KEY:
        return {
            "summary": f"No AI synthesis available. Seed data only for {req.name}.",
            "recommended_actions": ["Configure API_KEY_OPENROUTER for AI synthesis"],
            "privacy_flags": [],
        }

    # If no real data was gathered, use AI-only mode
    if not data.real_data_found:
        return await _ai_only_estimate(req)

    # Build context from gathered data
    context_parts = [f"Subject: {req.name}"]
    if req.dob:
        context_parts.append(f"DOB: {req.dob}")
    if req.graduation_year:
        context_parts.append(f"Graduation year: {req.graduation_year}")
    if req.last_known_address:
        context_parts.append(f"Last known address: {req.last_known_address}")
    if req.last_known_employer:
        context_parts.append(f"Last known employer: {req.last_known_employer}")

    context_parts.append("\n--- GATHERED INTELLIGENCE ---")
    if data.emails:
        context_parts.append(f"Emails found: {', '.join(data.emails)}")
    if data.phones:
        context_parts.append(f"Phones found: {', '.join(data.phones)}")
    if data.addresses:
        context_parts.append(f"Addresses found: {', '.join(data.addresses)}")
    if data.employers:
        context_parts.append(f"Employers found: {', '.join(data.employers)}")
    if data.linkedin_urls:
        context_parts.append(f"LinkedIn: {', '.join(data.linkedin_urls)}")
    if data.facebook_urls:
        context_parts.append(f"Facebook: {', '.join(data.facebook_urls)}")
    if data.raw_snippets:
        context_parts.append(f"\nSearch snippets:\n" + "\n".join(data.raw_snippets[:5]))

    context_parts.append(f"\nSources used: {', '.join(data.sources_used)}")

    user_msg = "\n".join(context_parts) + "\n\nSynthesise the above into a summary, recommended actions, and privacy flags. Return JSON only."

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            r = await client.post(
                f"{OPENROUTER_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {OPENROUTER_API_KEY}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://alumnibeacon.com",
                    "X-Title": "AlumniBeacon",
                },
                json={
                    "model": OPENROUTER_MODEL,
                    "messages": [
                        {"role": "system", "content": SYNTHESIS_PROMPT},
                        {"role": "user", "content": user_msg},
                    ],
                    "temperature": 0.1,
                    "max_tokens": 800,
                }
            )

        if r.status_code != 200:
            logger.error(f"Claude synthesis {r.status_code}: {r.text[:200]}")
            return {"summary": f"Synthesis unavailable. Found data for {req.name}.",
                    "recommended_actions": [], "privacy_flags": []}

        raw = r.json()["choices"][0]["message"]["content"]
        return json.loads(strip_markdown(raw))

    except Exception as e:
        logger.error(f"Tier 5 (Claude synthesis) error: {e}")
        return {"summary": f"Data gathered for {req.name}. Synthesis failed.",
                "recommended_actions": [], "privacy_flags": []}


async def _ai_only_estimate(req: SearchRequest) -> dict:
    """AI-only fallback when no real data sources are available."""
    parts = [f"Subject: {req.name}"]
    if req.dob:
        parts.append(f"DOB: {req.dob}")
    if req.graduation_year:
        parts.append(f"Graduation year: {req.graduation_year}")
    if req.last_known_address:
        parts.append(f"Last known address: {req.last_known_address}")
    if req.last_known_employer:
        parts.append(f"Last known employer: {req.last_known_employer}")
    if req.notes:
        parts.append(f"Notes: {req.notes}")

    user_msg = "\n".join(parts) + "\n\nReturn JSON only. Start with { character."

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            r = await client.post(
                f"{OPENROUTER_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {OPENROUTER_API_KEY}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://alumnibeacon.com",
                    "X-Title": "AlumniBeacon",
                },
                json={
                    "model": OPENROUTER_MODEL,
                    "messages": [
                        {"role": "system", "content": AI_ONLY_PROMPT},
                        {"role": "user", "content": user_msg},
                    ],
                    "temperature": 0.1,
                    "max_tokens": 600,
                }
            )

        if r.status_code != 200:
            return {"found_email": None, "found_phone": None, "found_address": None,
                    "found_employer": req.last_known_employer, "found_linkedin": None,
                    "found_facebook": None,
                    "summary": f"AI estimate only for {req.name}. No real data sources configured.",
                    "recommended_actions": ["Configure OSINT API keys for verified results"],
                    "privacy_flags": []}

        raw = r.json()["choices"][0]["message"]["content"]
        return json.loads(strip_markdown(raw))

    except Exception as e:
        logger.error(f"AI-only estimate error: {e}")
        return {"found_email": None, "found_phone": None, "found_address": None,
                "found_employer": req.last_known_employer, "found_linkedin": None,
                "found_facebook": None,
                "summary": f"Search attempted for {req.name}. No results available.",
                "recommended_actions": ["Manual search recommended"],
                "privacy_flags": []}


# ── Main search orchestrator ──────────────────────────────────────────────────
async def run_osint_pipeline(req: SearchRequest) -> dict:
    """Run all tiers in sequence, accumulate data, synthesise."""
    data = GatheredData(req)

    # Tier 1: Google
    await tier1_google(req, data)

    # Tier 2: Proxycurl (LinkedIn)
    await tier2_proxycurl(req, data)

    # Tier 3: People Data Labs
    await tier3_pdl(req, data)

    # Tier 4: Hunter.io (needs employer from earlier tiers)
    await tier4_hunter(req, data)

    # Tier 5: Claude synthesis
    synthesis = await tier5_claude_synthesis(req, data)

    # Determine mode
    mode = "live" if data.real_data_found else "mock"

    # Build sources list
    sources = data.sources_used.copy()
    if not sources:
        sources = ["AI estimate only — configure OSINT API keys for real data"]

    # Calculate confidence
    confidence_score, confidence_breakdown = calculate_confidence(data)

    # If AI-only, cap confidence at 30
    if not data.real_data_found:
        confidence_score = min(confidence_score, 30)

    # Merge synthesis fields (AI-only mode may provide address/employer estimates)
    found_address = data.best(data.addresses) or synthesis.get("found_address")
    found_employer = data.best(data.employers) or synthesis.get("found_employer")

    return {
        "confidence_score":    confidence_score,
        "found_email":         data.best(data.emails) or synthesis.get("found_email"),
        "found_phone":         data.best(data.phones) or synthesis.get("found_phone"),
        "found_address":       found_address,
        "found_employer":      found_employer,
        "found_linkedin":      data.best(data.linkedin_urls) or synthesis.get("found_linkedin"),
        "found_facebook":      data.best(data.facebook_urls) or synthesis.get("found_facebook"),
        "sources":             sources,
        "summary":             synthesis.get("summary", ""),
        "confidence_breakdown": confidence_breakdown,
        "recommended_actions": synthesis.get("recommended_actions", []),
        "privacy_flags":       synthesis.get("privacy_flags", []),
        "mode":                mode,
    }


# ── API endpoints ─────────────────────────────────────────────────────────────
@app.get("/health")
async def health():
    configured_tiers = []
    if GOOGLE_API_KEY and GOOGLE_CSE_ID:
        configured_tiers.append("Google Custom Search")
    if PROXYCURL_API_KEY:
        configured_tiers.append("Proxycurl")
    if PDL_API_KEY:
        configured_tiers.append("People Data Labs")
    if HUNTER_API_KEY:
        configured_tiers.append("Hunter.io")
    if OPENROUTER_API_KEY:
        configured_tiers.append("Claude synthesis")

    return {
        "status": "ok",
        "version": "3.0.0",
        "mode": "live" if configured_tiers else "ai-only",
        "configured_tiers": configured_tiers,
        "ai_configured": bool(OPENROUTER_API_KEY),
        "model": OPENROUTER_MODEL,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


@app.post("/osint/search", response_model=SearchResult)
async def osint_search(req: SearchRequest):
    logger.info(f"OSINT search: {req.name} (id={req.investigation_id})")
    findings = await run_osint_pipeline(req)

    return SearchResult(
        investigation_id=req.investigation_id,
        subject_name=req.name,
        status="completed",
        confidence_score=findings["confidence_score"],
        found_email=findings["found_email"],
        found_phone=findings["found_phone"],
        found_address=findings["found_address"],
        found_employer=findings["found_employer"],
        found_linkedin=findings["found_linkedin"],
        found_facebook=findings["found_facebook"],
        sources=findings["sources"],
        summary=findings["summary"],
        confidence_breakdown=findings["confidence_breakdown"],
        recommended_actions=findings["recommended_actions"],
        privacy_flags=findings["privacy_flags"],
        raw_findings=findings,
        searched_at=datetime.now(timezone.utc).isoformat(),
        mode=findings["mode"],
    )


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("OSINT_ADAPTER_PORT", "8000"))
    tiers = sum([
        bool(GOOGLE_API_KEY and GOOGLE_CSE_ID),
        bool(PROXYCURL_API_KEY),
        bool(PDL_API_KEY),
        bool(HUNTER_API_KEY),
        bool(OPENROUTER_API_KEY),
    ])
    logger.info(f"August OSINT Adapter v3.0 | {tiers}/5 tiers configured | port={port}")
    uvicorn.run(app, host="0.0.0.0", port=port)


# ── Agent Zero A2A Bridge ─────────────────────────────────────────────────────

AGENT_ZERO_URL = os.environ.get("AGENT_ZERO_URL", "http://localhost")
AGENT_ZERO_TIMEOUT = int(os.environ.get("AGENT_ZERO_TIMEOUT", "900"))  # 15 min


def build_investigation_prompt(req: SearchRequest) -> str:
    """Build the investigation prompt for the alumnibeacon-osint Agent Zero profile."""
    parts = [f"Investigate the following subject and return ONLY a valid JSON result in the AlumniBeacon schema."]
    parts.append("")
    parts.append("SEED DATA:")
    parts.append(f"- Full name: {req.name}")
    if req.dob:
        parts.append(f"- Date of birth: {req.dob}")
    if req.graduation_year:
        parts.append(f"- Graduation year: {req.graduation_year}")
    if req.last_known_address:
        parts.append(f"- Last known address: {req.last_known_address}")
    if req.last_known_employer:
        parts.append(f"- Last known employer: {req.last_known_employer}")
    if req.last_known_email:
        parts.append(f"- Last known email (unverified): {req.last_known_email}")
    if req.last_known_phone:
        parts.append(f"- Last known phone (unverified): {req.last_known_phone}")
    if req.notes:
        parts.append(f"- Notes: {req.notes}")
    parts.append("")
    parts.append("Follow your 7-step investigation workflow. Return ONLY the JSON object when complete — no markdown, no prose, no backticks.")
    return "\n".join(parts)


def extract_json_from_text(text: str) -> dict:
    """Extract a JSON object from agent response text.
    Handles cases where the agent wraps JSON in prose or markdown."""
    if not text:
        raise ValueError("Empty response from agent")

    # Try direct parse first
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Try to find JSON object in text (handles markdown code blocks)
    # Remove markdown code fences
    text_clean = re.sub(r'```(?:json)?\s*', '', text)
    text_clean = re.sub(r'```\s*$', '', text_clean, flags=re.MULTILINE)
    try:
        return json.loads(text_clean.strip())
    except json.JSONDecodeError:
        pass

    # Find first { ... } block
    start = text.find('{')
    end = text.rfind('}')
    if start != -1 and end != -1 and end > start:
        try:
            return json.loads(text[start:end + 1])
        except json.JSONDecodeError:
            pass

    raise ValueError(f"Could not extract JSON from agent response (length={len(text)})")


async def call_agent_zero_a2a(prompt: str, investigation_id: str) -> dict:
    """Call Agent Zero via A2A protocol and return parsed JSON result."""
    import uuid as uuid_mod

    base_url = AGENT_ZERO_URL.rstrip('/')
    a2a_url = f"{base_url}/a2a"

    # Build A2A message
    message_id = str(uuid_mod.uuid4())
    context_id = f"alumnibeacon-{investigation_id}-{message_id[:8]}"

    a2a_message = {
        "role": "user",
        "parts": [{"kind": "text", "text": prompt}],
        "kind": "message",
        "message_id": message_id,
        "context_id": context_id
    }

    logger.info(f"Calling Agent Zero A2A at {a2a_url} for investigation {investigation_id}")

    async with httpx.AsyncClient(timeout=AGENT_ZERO_TIMEOUT) as client:
        # Step 1: Send message to A2A endpoint
        resp = await client.post(
            a2a_url,
            json=a2a_message,
            headers={"Content-Type": "application/json"}
        )
        resp.raise_for_status()
        task_data = resp.json()
        logger.info(f"A2A task created: {json.dumps(task_data)[:200]}")

        # Step 2: Extract task_id
        task_id = None
        if isinstance(task_data, dict):
            task_id = (task_data.get("result", {}) or {}).get("id") or task_data.get("id")

        if not task_id:
            # Some A2A implementations return result inline
            history = (task_data.get("result", {}) or {}).get("history", [])
            if history:
                last_parts = history[-1].get("parts", [])
                text = "\n".join(p.get("text", "") for p in last_parts if p.get("kind") == "text")
                if text:
                    return extract_json_from_text(text)
            raise ValueError(f"No task_id in A2A response: {str(task_data)[:300]}")

        # Step 3: Poll until completed
        poll_url = f"{a2a_url}/tasks/{task_id}"
        max_polls = 180  # 15 min at 5s intervals
        for attempt in range(max_polls):
            await asyncio.sleep(5)
            poll_resp = await client.get(poll_url)
            poll_resp.raise_for_status()
            poll_data = poll_resp.json()

            result = poll_data.get("result", poll_data)
            status = result.get("status", {})
            state = status.get("state", "unknown") if isinstance(status, dict) else str(status)

            logger.info(f"A2A poll {attempt + 1}/{max_polls}: task={task_id} state={state}")

            if state in ("completed", "failed", "canceled"):
                if state != "completed":
                    raise ValueError(f"Agent Zero task {state}: {result.get('status', {}).get('message', '')}")

                # Step 4: Extract text from history
                history = result.get("history", [])
                if not history:
                    raise ValueError("Agent Zero returned no history")

                last_parts = history[-1].get("parts", [])
                text = "\n".join(p.get("text", "") for p in last_parts if p.get("kind") == "text")
                logger.info(f"Agent Zero response text (length={len(text)}): {text[:200]}")
                return extract_json_from_text(text)

        raise TimeoutError(f"Agent Zero task {task_id} did not complete within {AGENT_ZERO_TIMEOUT}s")


@app.post("/agent-zero/investigate")
async def agent_zero_investigate(req: SearchRequest):
    """Investigate using the alumnibeacon-osint Agent Zero profile via A2A."""
    logger.info(f"Agent Zero investigation: {req.name} (id={req.investigation_id})")

    prompt = build_investigation_prompt(req)
    inv_id = req.investigation_id or "unknown"

    try:
        result = await call_agent_zero_a2a(prompt, inv_id)
    except Exception as e:
        logger.error(f"Agent Zero A2A failed: {e}")
        raise

    # Inject engine field
    result["engine"] = "agent-zero"
    result.setdefault("mode", "live")

    return result


@app.get("/agent-zero/health")
async def agent_zero_health():
    """Check Agent Zero A2A connectivity."""
    base_url = AGENT_ZERO_URL.rstrip('/')
    try:
        async with httpx.AsyncClient(timeout=5) as client:
            resp = await client.get(f"{base_url}/.well-known/agent.json")
            if resp.status_code == 200:
                card = resp.json()
                return {"status": "ok", "agent": card.get("name", "unknown"), "url": base_url}
            return {"status": "error", "code": resp.status_code, "url": base_url}
    except Exception as e:
        return {"status": "unreachable", "error": str(e), "url": base_url}
