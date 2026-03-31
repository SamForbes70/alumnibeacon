# PRD: Option D — Dedicated AlumniBeacon OSINT Agent Zero Profile
**Version:** 1.0  
**Date:** 2026-03-31  
**Status:** Proposed  
**Author:** August (Agent Zero)

---

## 1. Executive Summary

Replace AlumniBeacon's deterministic Python OSINT adapter with an **adaptive AI agent** — a dedicated Agent Zero profile purpose-built for alumni investigation. Instead of calling fixed APIs in a fixed sequence, the AlumniBeacon OSINT Agent reasons about each subject, selects the right tools dynamically, follows leads, cross-references findings, and returns a structured JSON result in the exact schema AlumniBeacon expects.

This transforms AlumniBeacon from a "pipeline product" into an **intelligence product**.

---

## 2. Problem Statement

### What the Python Adapter Cannot Do

The current 5-tier Python adapter (v3.0) is a deterministic pipeline:

```
Google → Proxycurl → PDL → Hunter.io → Claude synthesis
```

It runs the same sequence for every subject regardless of what it finds. It cannot:

- **Follow leads** — if Google finds a LinkedIn URL, it doesn't then check that person's connections or employer page
- **Adapt to failure** — if PDL returns nothing, it doesn't try a different query strategy
- **Use browser automation** — can't scrape sites that don't have APIs (electoral rolls, White Pages, alumni directories)
- **Cross-reference intelligently** — can't notice that two sources disagree and investigate the discrepancy
- **Handle name variations** — can't try "John Smith", "J. Smith", "Jonathan Smith" systematically
- **Search Australian-specific sources** — can't query ABN Lookup, ASIC, state electoral rolls, or AU-specific directories
- **Learn from past investigations** — every search starts from zero

### What Agent Zero Can Do

Agent Zero with OSINT skills is an **adaptive intelligence**. Given a subject, it:

1. Reasons about the best search strategy for this specific person
2. Runs web searches, browser automation, code execution, and API calls
3. Evaluates what it finds and decides what to do next
4. Follows leads — a LinkedIn URL leads to an employer page leads to a phone number
5. Cross-references — confirms an address appears in multiple independent sources
6. Handles edge cases — name changes, common names, deceased subjects
7. Remembers successful strategies from past investigations

---

## 3. Vision

> **"AlumniBeacon powered by an AI investigator, not a data pipeline."**

Every investigation is handled by a dedicated AI agent that thinks like a skilled OSINT analyst — methodical, adaptive, privacy-aware, and Australian-focused. The agent uses every tool at its disposal to find the subject, then delivers a clean structured report.

---

## 4. How It Works — Technical Architecture

### 4.1 The A2A Bridge

Agent Zero exposes a **FastA2A-compatible HTTP API**. AlumniBeacon's Java backend calls this API directly — no new infrastructure required.

```
┌─────────────────────────────────────────────────────────┐
│                    AlumniBeacon                         │
│                                                         │
│  JobQueueScheduler.java                                 │
│    → AugustOsintAdapter.java                            │
│        → POST http://agent-zero:3000/a2a                │
│            body: { investigation JSON }                 │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Agent Zero                           │
│                                                         │
│  FastA2A endpoint receives investigation request        │
│    → Spawns AlumniBeacon OSINT Profile agent            │
│        → Agent runs investigation using OSINT skills    │
│            (web search, browser, terminal, APIs)        │
│        → Returns structured JSON in AlumniBeacon schema │
│    → A2A response delivered to Java caller              │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    AlumniBeacon                         │
│                                                         │
│  AugustOsintAdapter.java receives JSON response         │
│    → InvestigationService.parseOsintResult()            │
│        → OsintResultDto populated                       │
│            → Displayed in structured UI cards           │
└─────────────────────────────────────────────────────────┘
```

### 4.2 Context Isolation

Each investigation gets a **fresh agent context** via `reset: true` in the A2A call. This prevents:
- Investigation A's findings bleeding into Investigation B
- Memory accumulation causing context window overflow
- Cross-tenant data leakage

```java
// Each investigation call resets the agent context
POST /a2a
{
  "message": "...",
  "reset": true   // ← fresh context per investigation
}
```

### 4.3 Structured Output Enforcement

The dedicated profile's system prompt **mandates JSON output** in AlumniBeacon's exact schema. The agent is instructed:
- Final response MUST be valid JSON only
- No markdown, no prose, no explanation
- Exact field names matching `OsintResultDto`
- Null for fields not found (never invented values)

The Java adapter validates the JSON and falls back to the Python adapter if parsing fails.

### 4.4 Iteration Budget

Each investigation is capped at **30 tool calls** maximum. The system prompt instructs the agent to:
- Prioritise the highest-value tools first
- Stop and report when budget is 80% consumed
- Never loop indefinitely on a single source

---

## 5. The AlumniBeacon OSINT Agent Profile

### 5.1 Profile Location

```
agents/alumnibeacon-osint/
├── prompts/
│   ├── agent.system.role.md          # Who the agent is
│   ├── agent.system.tools.md         # Which tools to use and how
│   ├── agent.system.output.md        # Mandatory JSON output schema
│   └── agent.system.compliance.md   # Privacy Act 1988 / GDPR rules
└── profile.json                      # Profile metadata
```

### 5.2 System Prompt Design

#### `agent.system.role.md`
```markdown
You are the AlumniBeacon OSINT Investigator — a specialist AI agent
for locating alumni and former students for educational institutions.

Your mission: Given a subject's seed information, find their current
contact details, location, employer, and social media presence using
open-source intelligence techniques.

You operate within Australian Privacy Act 1988 boundaries. You are
engaged by licensed educational institutions for legitimate alumni
reconnection purposes. You do not assist with stalking, harassment,
or any purpose beyond alumni reconnection.

You are methodical, adaptive, and honest. You report what you find
and clearly distinguish verified data from estimates.
```

#### `agent.system.tools.md`
```markdown
## Investigation Workflow

1. ANALYSE seed data — what do we know? What's the best starting point?
2. SEARCH broadly — web search for name + location + employer combinations
3. FIND profiles — LinkedIn, Facebook, Instagram, Twitter/X
4. ENRICH — visit profiles, extract current employer, location, contact info
5. VERIFY — cross-reference findings across 2+ independent sources
6. AUSTRALIAN SOURCES — check ABN Lookup, White Pages AU, electoral roll hints
7. SYNTHESISE — compile all verified findings into the output schema

## Tool Priority Order
1. Web search (always first — broad, fast, free)
2. Browser automation (for pages that need rendering)
3. LinkedIn profile lookup
4. Facebook profile lookup  
5. ABN Lookup (if employer known)
6. White Pages Australia
7. Code execution (for data processing, regex extraction)

## Budget Rule
You have a maximum of 30 tool calls per investigation.
At 24 tool calls, stop gathering and begin synthesis.
Never exceed 30 tool calls.
```

#### `agent.system.output.md`
```markdown
## MANDATORY OUTPUT FORMAT

Your FINAL response (when calling the `response` tool) MUST be a
valid JSON object and NOTHING ELSE. No markdown. No explanation.
No backticks. Start with { and end with }.

Required schema:
{
  "confidence_score": <integer 0-100>,
  "found_email": <string or null>,
  "found_phone": <string or null>,
  "found_address": <string or null>,
  "found_employer": <string or null>,
  "found_linkedin": <full URL string or null>,
  "found_facebook": <full URL string or null>,
  "sources": [<list of source names used>],
  "summary": <2-3 sentence narrative of findings>,
  "confidence_breakdown": {
    "name_match": <0-25>,
    "location_match": <0-25>,
    "professional_match": <0-25>,
    "contact_verified": <0-25>
  },
  "recommended_actions": [<list of action strings>],
  "privacy_flags": [<list of privacy concern strings>],
  "mode": "live",
  "tool_calls_used": <integer>
}

CRITICAL RULES:
- NEVER invent email addresses, phone numbers, or social media URLs
- Set fields to null if not found — do not guess
- found_linkedin and found_facebook must be full URLs (https://...)
- confidence_score must reflect actual evidence quality
- If subject cannot be found, return all contact fields as null
  with an honest summary and confidence_score <= 20
```

#### `agent.system.compliance.md`
```markdown
## Privacy and Legal Compliance

You operate under Australian Privacy Act 1988 and GDPR principles.

PERMITTED:
- Searching publicly available information
- LinkedIn profiles set to public
- Facebook profiles set to public
- Business registrations (ABN Lookup, ASIC)
- Electoral roll data where legally accessible
- News articles and public records
- University alumni directories (public sections)

NOT PERMITTED:
- Accessing private/locked social media profiles
- Purchasing or accessing leaked databases
- Any dark web sources
- Medical, financial, or legal records
- Accessing systems without authorisation

PRIVACY FLAGS — add to privacy_flags array if:
- Subject appears to have deliberately removed online presence
- Subject has changed name (possible domestic violence situation)
- Subject is a minor or appears to be under 18
- Subject appears deceased
- Multiple conflicting identities found (possible fraud)
```

---

## 6. Java Integration Changes

### 6.1 `AugustOsintAdapter.java` — Dual Mode

The adapter gains a **mode selector**: Agent Zero (primary) or Python FastAPI (fallback).

```java
@Component
@Slf4j
public class AugustOsintAdapter {

    private final WebClient agentZeroClient;   // Agent Zero A2A
    private final WebClient pythonClient;       // Python FastAPI fallback
    private final boolean agentZeroEnabled;

    public Mono<String> search(String payloadJson) {
        if (agentZeroEnabled) {
            return searchViaAgentZero(payloadJson)
                .onErrorResume(e -> {
                    log.warn("Agent Zero failed, falling back to Python adapter: {}", e.getMessage());
                    return searchViaPython(payloadJson);
                });
        }
        return searchViaPython(payloadJson);
    }

    private Mono<String> searchViaAgentZero(String payloadJson) {
        // Build A2A message from investigation payload
        String a2aMessage = buildA2AMessage(payloadJson);

        return agentZeroClient.post()
            .uri("/a2a")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "message", a2aMessage,
                "reset", true   // fresh context per investigation
            ))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofMinutes(15))  // Agent Zero needs more time
            .map(this::extractJsonFromA2AResponse);
    }

    private String buildA2AMessage(String payloadJson) {
        // Convert investigation JSON to natural language instruction
        // + append mandatory JSON output schema
        return "Investigate the following subject and return ONLY valid JSON:\n\n"
             + payloadJson
             + "\n\nReturn the result in the AlumniBeacon schema. JSON only. No markdown.";
    }

    private String extractJsonFromA2AResponse(String a2aResponse) {
        // A2A response wraps the agent's final `response` tool output
        // Extract the JSON from the response wrapper
        // Validate it matches OsintResultDto schema
        // Return raw JSON string for InvestigationService.parseOsintResult()
    }
}
```

### 6.2 New `application.properties` Keys

```properties
# Agent Zero A2A integration
agent.zero.enabled=true
agent.zero.url=http://localhost:3000
agent.zero.timeout.minutes=15
agent.zero.profile=alumnibeacon-osint
agent.zero.max.iterations=30

# Python adapter (fallback)
august.adapter.url=http://localhost:8000
august.adapter.timeout=120000
```

### 6.3 Timeout Strategy

| Scenario | Timeout |
|---|---|
| Agent Zero investigation | 15 minutes |
| Agent Zero health check | 5 seconds |
| Python adapter fallback | 2 minutes |
| Job queue stuck-job reset | 20 minutes |

The job queue's stuck-job recovery threshold must be raised from its current value to 20 minutes to accommodate Agent Zero investigations.

---

## 7. OSINT Skills to Load in Profile

The dedicated profile pre-loads these Agent Zero skills:

| Skill | Purpose |
|---|---|
| `advanced-web-search` | Google dorking, specialized search operators |
| `social-media-intelligence` | Platform-specific search, profile extraction |
| `phone-number-osint` | Phone number lookup and carrier identification |
| `username-enumeration` | Cross-platform username search |
| `profile-correlation` | Link profiles across platforms |
| `evidence-packaging` | Structure findings with source citations |
| `false-positive-detection` | Avoid misidentifying subjects |
| `result-verification` | Cross-reference validation |

Australian-specific additions (to be created):
- `au-white-pages` — White Pages Australia search patterns
- `au-abn-lookup` — ABN Lookup for employer verification
- `au-electoral` — Electoral roll search strategies
- `au-linkedin-search` — AU-specific LinkedIn search operators

---

## 8. Confidence Scoring in Agent Zero Mode

The agent calculates confidence using the same 100-point algorithm as the Python adapter, but with additional signals:

| Signal | Points | Agent Zero Advantage |
|---|---|---|
| Data points found | 30 | Can find more fields via browser automation |
| Cross-source agreement | 25 | Can verify across more sources |
| Verification level | 25 | Can visit and confirm profile pages directly |
| Source quality | 10 | Can assess source credibility |
| Recency | 10 | Can check "last active" dates on profiles |

Agent Zero investigations are expected to achieve **15-25 points higher** confidence than the Python adapter for the same subject, due to adaptive follow-up and browser access.

---

## 9. Fallback Strategy

```
Investigation submitted
    │
    ▼
Agent Zero available? ──No──→ Python adapter (fast, structured)
    │
   Yes
    ▼
Agent Zero investigation (up to 15 min)
    │
    ├── Success → parse JSON → display results
    │
    └── Failure (timeout/error)
            │
            ▼
        Python adapter fallback (automatic)
            │
            ▼
        Results displayed with "Standard Search" label
```

The UI shows which engine produced the result:
- 🤖 **Deep Investigation** (Agent Zero) — amber badge
- ⚡ **Standard Search** (Python adapter) — blue badge

---

## 10. User Experience Changes

### Investigation Creation Form
Add a **"Search Depth"** toggle:
- ⚡ **Standard** (60 sec) — Python adapter, structured APIs
- 🤖 **Deep Investigation** (5-15 min) — Agent Zero, adaptive AI

Default: Standard. Deep Investigation available on Professional/Enterprise plans.

### Investigation Status Page
New status states for Agent Zero investigations:
- `PENDING` → `AGENT_THINKING` → `AGENT_SEARCHING` → `COMPLETED`

HTMX polling shows live status messages:
```
🤖 AI investigator is analysing seed data...
🔍 Searching web for John Smith...
🔗 Found LinkedIn profile, enriching...
📊 Cross-referencing findings...
✅ Investigation complete
```

### Results Display
Agent Zero results show additional metadata:
- Tool calls used: `18 / 30`
- Investigation engine: `Agent Zero (alumnibeacon-osint v1.0)`
- Investigation duration: `4m 32s`

---

## 11. Implementation Roadmap

### Phase 1 — Profile Creation (Week 1)
- [ ] Create `agents/alumnibeacon-osint/` directory structure
- [ ] Write 4 system prompt fragments (role, tools, output, compliance)
- [ ] Write `profile.json` metadata
- [ ] Test profile manually via Agent Zero web UI
- [ ] Verify JSON output schema compliance
- [ ] Test context isolation (reset: true)

### Phase 2 — Java Integration (Week 2)
- [ ] Update `AugustOsintAdapter.java` with dual-mode (A2A + Python fallback)
- [ ] Add `extractJsonFromA2AResponse()` parser
- [ ] Add new `application.properties` keys
- [ ] Raise job queue stuck-job timeout to 20 minutes
- [ ] Add `agent.zero.enabled` feature flag
- [ ] Integration test: submit investigation → Agent Zero → parse result

### Phase 3 — UI Enhancements (Week 3)
- [ ] Add "Search Depth" toggle to investigation creation form
- [ ] Add `AGENT_THINKING` / `AGENT_SEARCHING` status states
- [ ] Update HTMX polling to show live status messages
- [ ] Add investigation engine badge to results display
- [ ] Add tool calls used / duration metadata

### Phase 4 — Australian Skills (Week 4)
- [ ] Create `au-white-pages` skill
- [ ] Create `au-abn-lookup` skill
- [ ] Create `au-linkedin-search` skill
- [ ] Load skills into `alumnibeacon-osint` profile
- [ ] End-to-end test with real Australian subjects

### Phase 5 — Production Hardening (Week 5)
- [ ] Load testing: 5 concurrent Agent Zero investigations
- [ ] Cost monitoring: average tool calls per investigation
- [ ] Fallback testing: Agent Zero timeout → Python adapter
- [ ] Privacy compliance review of all sources used
- [ ] Deploy to staging, pilot with 3 education centre customers

---

## 12. Cost Model

### Per Investigation Cost Estimate

| Component | Cost | Notes |
|---|---|---|
| Agent Zero LLM calls | ~$0.05–0.20 | 30 iterations × ~$0.005/call |
| Web search API | ~$0.01–0.05 | 5-10 Google CSE queries |
| Browser automation | $0 | Local Playwright, no API cost |
| Python adapter fallback | ~$0.02 | Only on Agent Zero failure |
| **Total per investigation** | **~$0.08–0.30** | vs $0.15 Python-only |

### Pricing Recommendation

| Plan | Search Type | Credits | Price |
|---|---|---|---|
| Starter | Standard only | 50 | $49 AUD |
| Standard | Standard + 10 Deep | 200 | $149 AUD |
| Professional | Standard + 50 Deep | 500 | $299 AUD |
| Enterprise | Unlimited Deep | 2,000 | $999 AUD |

Deep Investigation = 3 credits. Standard = 1 credit.

---

## 13. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Agent Zero produces non-JSON output | Medium | High | Strict output prompt + JSON validation + Python fallback |
| Investigation takes >15 minutes | Low | Medium | Hard timeout + fallback |
| Agent Zero hallucinates contact details | Low | High | Output prompt explicitly forbids invention; Java validates plausibility |
| Context bleed between investigations | Low | High | `reset: true` per investigation |
| Cost overrun from runaway tool loops | Low | Medium | 30 tool call hard cap in system prompt |
| Agent Zero instance unavailable | Medium | Medium | Python adapter fallback always available |
| Privacy Act breach via prohibited sources | Low | Critical | Compliance prompt + source whitelist |

---

## 14. Success Criteria

### Technical Gates (all must pass before production)
- [ ] Agent Zero returns valid JSON in AlumniBeacon schema 95%+ of the time
- [ ] Average investigation completes in < 8 minutes
- [ ] Python fallback triggers correctly on Agent Zero failure
- [ ] Zero cross-tenant data leakage (context isolation verified)
- [ ] No invented contact details in 100 test investigations
- [ ] Cost per investigation < $0.35 AUD average

### Quality Gates
- [ ] Agent Zero finds 25%+ more data points than Python adapter on same subjects
- [ ] Confidence scores 15+ points higher on average
- [ ] Zero privacy compliance violations in audit
- [ ] 3 pilot customers report results as "significantly better" than standard search

---

## 15. What This Makes AlumniBeacon

Every competitor in the alumni reconnection space uses fixed API pipelines — the same deterministic sequence of database lookups. AlumniBeacon with Option D becomes the **only platform with an adaptive AI investigator** that thinks, adapts, and follows leads like a human OSINT analyst.

This is a genuine moat. The profile can be continuously improved. The skills can be expanded. The agent gets better with every investigation it completes. No competitor can replicate this without building the same agentic infrastructure.

> **The Python adapter finds data. The Agent Zero profile finds people.**
