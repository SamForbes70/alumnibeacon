"""
August OSINT Adapter v2.1
FastAPI wrapper calling OpenRouter/Claude for real OSINT intelligence.
"""
import json
import logging
import os
from datetime import datetime, timezone
from typing import Optional

import httpx
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

OPENROUTER_API_KEY = os.environ.get("API_KEY_OPENROUTER", "")
OPENROUTER_MODEL = os.environ.get("OSINT_MODEL", "anthropic/claude-sonnet-4-5")
OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="August OSINT Adapter", version="2.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"]
)

SYSTEM_PROMPT = (
    "You are an expert OSINT investigator for alumni reconnection.\n"
    "Operate within GDPR, Australian Privacy Act 1988, and CCPA boundaries.\n\n"
    "Return ONLY a valid JSON object with NO markdown fences, NO backticks, NO explanation.\n"
    "JSON structure required:\n"
    "{confidence_score: 0-100, found_email: str|null, found_phone: str|null, "
    "found_address: str|null, found_employer: str|null, found_linkedin: str|null, "
    "found_facebook: str|null, sources: [str], summary: str, "
    "confidence_breakdown: {name_match:0-25, location_match:0-25, professional_match:0-25, contact_verified:0-25}, "
    "recommended_actions: [str], privacy_flags: [str]}\n\n"
    "CRITICAL: Raw JSON only. No markdown. No backticks. Start response with { character."
)


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


def strip_markdown(raw: str) -> str:
    """Remove markdown code fences from LLM response."""
    s = raw.strip()
    if s.startswith("`"):
        lines = s.splitlines()
        inner = lines[1:-1] if lines and lines[-1].strip() == "```" else lines[1:]
        s = "\n".join(inner).strip()
    return s


def mock_response(req: SearchRequest) -> dict:
    dash = req.name.lower().replace(" ", "-")
    return {
        "confidence_score": 45,
        "found_email": None,
        "found_phone": None,
        "found_address": req.last_known_address or "Unknown",
        "found_employer": req.last_known_employer,
        "found_linkedin": f"https://linkedin.com/in/{dash}",
        "found_facebook": None,
        "sources": ["Mock - configure API_KEY_OPENROUTER for live results"],
        "summary": f"Mock result for {req.name}. Set API_KEY_OPENROUTER env var for live OSINT.",
        "confidence_breakdown": {"name_match": 10, "location_match": 10, "professional_match": 10, "contact_verified": 15},
        "recommended_actions": ["Configure OpenRouter API key"],
        "privacy_flags": [],
        "_mode": "mock"
    }


async def call_openrouter(req: SearchRequest) -> dict:
    if not OPENROUTER_API_KEY:
        return mock_response(req)

    parts = [f"Subject: {req.name}"]
    if req.dob:
        parts.append(f"DOB: {req.dob}")
    if req.graduation_year:
        parts.append(f"Grad year: {req.graduation_year}")
    if req.last_known_address:
        parts.append(f"Last address: {req.last_known_address}")
    if req.last_known_email:
        parts.append(f"Last email: {req.last_known_email}")
    if req.last_known_employer:
        parts.append(f"Last employer: {req.last_known_employer}")
    if req.notes:
        parts.append(f"Notes: {req.notes}")
    user_msg = "\n".join(parts) + "\n\nReturn JSON only. Start with { character."

    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            r = await client.post(
                f"{OPENROUTER_BASE_URL}/chat/completions",
                headers={
                    "Authorization": f"Bearer {OPENROUTER_API_KEY}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://alumnibeacon.com",
                    "X-Title": "AlumniBeacon"
                },
                json={
                    "model": OPENROUTER_MODEL,
                    "messages": [
                        {"role": "system", "content": SYSTEM_PROMPT},
                        {"role": "user", "content": user_msg}
                    ],
                    "temperature": 0.1,
                    "max_tokens": 1500
                }
            )
        if r.status_code != 200:
            logger.error(f"OpenRouter {r.status_code}: {r.text[:200]}")
            return mock_response(req)

        raw = r.json()["choices"][0]["message"]["content"]
        cleaned = strip_markdown(raw)
        result = json.loads(cleaned)
        result["_mode"] = "live"
        logger.info(f"Live result for {req.name}: confidence={result.get('confidence_score')}%")
        return result

    except json.JSONDecodeError as e:
        logger.error(f"JSON parse error: {e} | raw: {raw[:300]}")
        return mock_response(req)
    except Exception as e:
        logger.error(f"OpenRouter error: {e}")
        return mock_response(req)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "version": "2.1.0",
        "mode": "live" if OPENROUTER_API_KEY else "mock",
        "api_configured": bool(OPENROUTER_API_KEY),
        "model": OPENROUTER_MODEL,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }


@app.post("/osint/search", response_model=SearchResult)
async def osint_search(req: SearchRequest):
    logger.info(f"Search: {req.name}")
    findings = await call_openrouter(req)
    mode = findings.pop("_mode", "live")
    return SearchResult(
        investigation_id=req.investigation_id,
        subject_name=req.name,
        status="completed",
        confidence_score=findings.get("confidence_score", 0),
        found_email=findings.get("found_email"),
        found_phone=findings.get("found_phone"),
        found_address=findings.get("found_address"),
        found_employer=findings.get("found_employer"),
        found_linkedin=findings.get("found_linkedin"),
        found_facebook=findings.get("found_facebook"),
        sources=findings.get("sources", []),
        summary=findings.get("summary", ""),
        confidence_breakdown=findings.get("confidence_breakdown", {}),
        recommended_actions=findings.get("recommended_actions", []),
        privacy_flags=findings.get("privacy_flags", []),
        raw_findings=findings,
        searched_at=datetime.now(timezone.utc).isoformat(),
        mode=mode
    )


if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("OSINT_ADAPTER_PORT", "8000"))
    logger.info(f"Mode: {'LIVE' if OPENROUTER_API_KEY else 'MOCK'} | Model: {OPENROUTER_MODEL}")
    uvicorn.run(app, host="0.0.0.0", port=port)
