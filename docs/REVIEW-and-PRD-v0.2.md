# AlumniBeacon — Complete System Review & PRD v0.2

**Review Date:** 31 March 2026  
**Reviewer:** August AI  
**Codebase:** Spring Boot 3.4.1 / Java 21 + Python FastAPI OSINT Adapter  
**Status:** Pre-production — functional skeleton, significant gaps before commercial readiness

---

## PART 1 — WHAT IS ACTUALLY BUILT

### 1.1 Functional Capabilities (Confirmed Working)

#### ✅ Authentication & Multi-Tenancy
| Capability | Implementation | Notes |
|---|---|---|
| User registration | `AuthController` + `AuthService` | Creates tenant + admin user atomically |
| User login | JWT issued, stored in httpOnly cookie | 24-hour expiry |
| JWT validation | `JwtAuthFilter` on every request | Cookie-based, not Bearer header |
| Multi-tenancy | `tenant_id` on all records | Application-level isolation (not DB RLS) |
| 4 user roles | `SUPER_ADMIN, ADMIN, INVESTIGATOR, VIEWER` | Defined but only ADMIN enforced in practice |
| Logout | Cookie cleared via `/logout` | Spring Security default |

#### ✅ Investigation Management
| Capability | Implementation | Notes |
|---|---|---|
| Create investigation | Form → `POST /investigations` | 8 input fields captured |
| List investigations | `GET /investigations` | Tenant-scoped, ordered by date |
| View investigation detail | `GET /investigations/{id}` | Shows status, confidence, raw JSON |
| Delete investigation | `POST /investigations/{id}/delete` | Cascades to job_queue |
| Retry failed investigation | `POST /investigations/{id}/retry` | Re-queues job, resets status |
| Credit gate on create | `CreditService.hasCredits()` | Blocks if 0 credits |
| HTMX live polling | `GET /investigations/{id}/status` | Polls every 5s, reloads on completion |

#### ✅ Job Queue & OSINT Processing
| Capability | Implementation | Notes |
|---|---|---|
| DB-backed job queue | `job_queue` table (SQLite) | NOT Redis — polling-based |
| Queue scheduler | `@Scheduled(fixedDelay=5000)` | Processes 1 job per 5-second tick |
| Stuck job recovery | Resets PROCESSING jobs >5 min old | Up to 3 retry attempts |
| OSINT adapter call | `AugustOsintAdapter` → WebClient | Calls Python FastAPI at `AUGUST_ADAPTER_URL` |
| Confidence score extraction | Parsed from JSON result | Stored as integer on investigation |
| Job status lifecycle | `PENDING → PROCESSING → COMPLETED/FAILED` | 3 states (not 5 as PRD specifies) |

#### ✅ OSINT Adapter (Python FastAPI)
| Capability | Implementation | Notes |
|---|---|---|
| AI-powered OSINT | OpenRouter → Claude claude-sonnet-4-20250514 | LLM generates plausible findings |
| Mock mode | Returns placeholder data if no API key | Safe fallback |
| Structured JSON output | 12-field schema enforced | email, phone, address, employer, linkedin, facebook, sources, summary, confidence_breakdown, recommended_actions, privacy_flags |
| Health endpoint | `GET /health` | Reports mode (live/mock) |
| Markdown stripping | Handles LLM code-fence responses | Robust JSON extraction |

#### ✅ Credit System
| Capability | Implementation | Notes |
|---|---|---|
| Credit balance check | `CreditService.hasCredits()` | Per-tenant |
| Credit deduction | `CreditService.deductCredits()` | 1 credit per search |
| Credit refund on failure | `CreditService.refundCredits()` | Called on job failure |
| Admin credit top-up | `CreditService.addCredits()` | No UI — admin API only |
| Credit display | Admin panel shows remaining/used | Visual progress bar |

#### ✅ PDF Report Generation
| Capability | Implementation | Notes |
|---|---|---|
| Branded PDF | iText 8.0.5 | AlumniBeacon colours (charcoal, magenta, gold) |
| Subject details section | Name, DOB, address, employer, investigation ID | |
| Findings section | Parses result_json for email, phone, address, LinkedIn, employer, summary | |
| Confidence section | Score + HIGH/MEDIUM/LOW label with colour coding | Threshold: 70%=HIGH, 40%=MEDIUM |
| Sources section | Lists data sources from result JSON | |
| Legal footer | Privacy Act 1988 disclaimer | |
| Download endpoint | `GET /investigations/{id}/report` | Only for COMPLETED investigations |

#### ✅ Admin Panel
| Capability | Implementation | Notes |
|---|---|---|
| Organisation settings | Update org name | contactEmail field referenced but not in model |
| Team member list | Shows all users in tenant | |
| Remove user | `POST /admin/users/{id}/remove` | Cannot remove self |
| Credit display | Balance + usage bar | |
| Usage stats | Total/completed investigations, success rate | |
| Billing CTA | Links to `/billing` | Page does not exist yet |

#### ✅ UI / Frontend
| Capability | Implementation | Notes |
|---|---|---|
| Brand design system | Tailwind CSS with AlumniBeacon palette | Charcoal, magenta, gold, cream |
| Playfair Display typography | Google Fonts | Serif headings |
| Responsive sidebar layout | Fixed 256px sidebar | Not mobile-responsive |
| Status badges | Colour-coded PENDING/PROCESSING/COMPLETED/FAILED | |
| Confidence bar | Visual progress bar with colour thresholds | |
| HTMX integration | Live polling, no full-page refresh | |
| Empty states | Friendly empty state on dashboard and list | |
| Flash messages | Success/error messages via RedirectAttributes | |

#### ✅ Infrastructure
| Capability | Implementation | Notes |
|---|---|---|
| SQLite database | WAL mode, foreign keys enabled | Single-file, no external DB needed |
| Flyway migrations | V1__initial_schema.sql | Schema versioned |
| Docker support | Dockerfile + docker-compose.yml | Supervisor manages both processes |
| Render deployment | render.yaml | Cloud deployment configured |
| Actuator health | `/actuator/health` | Minimal exposure |
| Audit log table | Schema exists | Nothing writes to it yet |

---

### 1.2 Non-Functional Capabilities (Confirmed)

| NFR | Status | Detail |
|---|---|---|
| **Security — JWT** | ✅ Implemented | httpOnly cookie, 24h expiry |
| **Security — CSRF** | ✅ Implemented | Spring Security CSRF tokens on forms |
| **Security — Password hashing** | ✅ Implemented | BCrypt via Spring Security |
| **Security — Tenant isolation** | ⚠️ Partial | Application-level only; no DB-level RLS |
| **Privacy — Audit table** | ⚠️ Schema only | Table exists, nothing writes to it |
| **Privacy — Data retention** | ❌ Missing | No auto-delete after 90 days |
| **Performance — Async jobs** | ✅ Implemented | Background scheduler, non-blocking UI |
| **Performance — Concurrent jobs** | ⚠️ Limited | Processes 1 job per 5s tick; no parallelism |
| **Scalability — SQLite** | ⚠️ Dev-only | SQLite unsuitable for multi-tenant production |
| **Reliability — Job retry** | ✅ Implemented | 3 attempts with stuck-job recovery |
| **Observability — Logging** | ✅ Implemented | SLF4J throughout, structured log messages |
| **Observability — Metrics** | ❌ Missing | No Prometheus/Micrometer metrics |
| **Compliance — Privacy notice** | ✅ In UI | Shown on investigation creation form |
| **Compliance — Legal footer** | ✅ In PDF | Privacy Act 1988 disclaimer |
| **Deployment — Docker** | ✅ Implemented | Multi-process via supervisord |
| **Deployment — Render** | ✅ Configured | render.yaml present |

---

## PART 2 — WHAT IS MISSING (GAP ANALYSIS)

### 2.1 Critical Gaps (Blockers for Production)

#### 🔴 GAP-01: OSINT is AI-Hallucinated, Not Real Data
**What exists:** The OSINT adapter sends subject details to Claude and asks it to "investigate". Claude generates plausible-sounding but entirely fabricated results — email addresses, phone numbers, LinkedIn URLs, employers that do not exist.

**What's needed:** Integration with real data sources:
- People Data Labs API (person enrichment)
- Proxycurl (LinkedIn data)
- Australian electoral roll / White Pages
- Google/Bing search API for web presence
- HaveIBeenPwned / breach databases for email verification

**Impact:** The core product promise — finding real people — does not work. Every result is fictional.

#### 🔴 GAP-02: SecurityConfig.java is Missing
**What exists:** The file is referenced throughout the codebase but does not exist on disk.

**What's needed:** The Spring Security configuration class that defines:
- Which routes require authentication
- JWT filter chain registration
- CORS configuration
- CSRF settings
- Role-based access control rules

**Impact:** Application may not start or may have open/broken security.

#### 🔴 GAP-03: No Structured Results Display
**What exists:** The investigation detail page dumps raw JSON into a `<pre>` tag.

**What's needed:** Parsed, human-readable display of:
- Current name and any aliases
- Residential address (current and historical)
- Phone numbers (mobile, landline)
- Email addresses
- Social media handles (LinkedIn, Facebook, Instagram, Twitter/X)
- Current employer and role
- Associations and connections
- Recommended actions

**Impact:** The product is unusable for non-technical staff. A school alumni officer cannot read raw JSON.

#### 🔴 GAP-04: SQLite Not Suitable for Production
**What exists:** SQLite with WAL mode.

**What's needed:** PostgreSQL for:
- Concurrent write access (multiple users/jobs)
- Row-level security for true tenant isolation
- Production-grade reliability and backup
- Connection pooling

**Impact:** Data corruption risk under concurrent load; cannot scale beyond single instance.

#### 🔴 GAP-05: No Billing / Stripe Integration
**What exists:** `stripe_customer_id` and `stripe_subscription_id` fields on Tenant model. A "Top Up Credits" button linking to `/billing` (404).

**What's needed:** Full Stripe integration:
- Checkout session for credit purchase
- Subscription management
- Webhook handler for payment events
- Customer portal for plan management

**Impact:** No revenue. Cannot charge customers.

---

### 2.2 Major Gaps (Required for MVP)

#### 🟠 GAP-06: 5-Stage Workflow Not Implemented
The PRD specifies: Initiation → Investigation → Analysis → QC → Delivery.
Actual implementation: PENDING → PROCESSING → COMPLETED (3 states).
Missing: Analysis stage, QC stage, false positive detection, proper confidence scoring algorithm.

#### 🟠 GAP-07: No Real Confidence Scoring
The confidence score is whatever the LLM returns (0-100). There is no multi-factor algorithm as specified in the PRD (data points 30%, cross-references 25%, verification 25%, recency 10%, source quality 10%).

#### 🟠 GAP-08: No Batch / CSV Upload
The PRD specifies bulk CSV upload of 1,000 records. Not implemented. Only single-record manual entry exists.

#### 🟠 GAP-09: No Group Investigation
The user requirement is to search for "an individual or a group of people". No group/batch investigation capability exists.

#### 🟠 GAP-10: No Email Notifications
No email sent when investigation completes. Users must manually check the dashboard.

#### 🟠 GAP-11: Investigations List Filtering/Search Broken
The list template has HTMX hooks for search-by-name and status filter, but the controller does not accept or process these query parameters. The filters do nothing.

#### 🟠 GAP-12: Pagination Not Wired
The list template has pagination UI but the controller passes no `totalPages`, `currentPage`, or `pageSize` to the model. Pagination is non-functional.

#### 🟠 GAP-13: No User Invite System
Admin panel shows "Invite functionality coming soon". Cannot add team members without direct database access.

#### 🟠 GAP-14: No Password Reset
No forgot-password flow. If a user loses their password, they cannot recover access.

---

### 2.3 Notable Gaps (Important but Deferrable)

| ID | Gap | Priority |
|---|---|---|
| GAP-15 | No alumni self-claim portal | P1 |
| GAP-16 | No CRM integrations (Salesforce, HubSpot, Blackbaud) | P2 |
| GAP-17 | No REST API for institutional integrations | P2 |
| GAP-18 | No WebSocket (HTMX polling is acceptable substitute) | P2 |
| GAP-19 | No scheduled/recurring searches | P2 |
| GAP-20 | No SUPER_ADMIN portal for platform management | P1 |
| GAP-21 | Audit logs table never written to | P1 |
| GAP-22 | No data retention / auto-delete after 90 days | P1 |
| GAP-23 | No rate limiting on API endpoints | P1 |
| GAP-24 | No mobile-responsive layout | P2 |
| GAP-25 | contactEmail field in admin UI but not in Tenant model | P0 (bug) |
| GAP-26 | currentUser.userId reference in admin template incorrect | P0 (bug) |
| GAP-27 | No phone number field displayed in investigation detail | P0 (bug) |
| GAP-28 | No Facebook/Instagram/Twitter display in UI | P1 |
| GAP-29 | No magic link / SSO authentication | P2 |
| GAP-30 | No benchmark reports or analytics dashboard | P2 |

---

### 2.4 Template Bugs (Immediate Fixes Required)

```
BUG-01: admin/index.html line ~120
  ${currentUser.userId} — should be ${currentUserId} (model attribute name)

BUG-02: admin/index.html — contactEmail
  ${tenant.contactEmail} — Tenant model has no contactEmail field
  Fix: Add contactEmail to Tenant model or remove from template

BUG-03: investigation/list.html — pagination variables
  ${totalPages}, ${currentPage}, ${pageSize}, ${completedCount}, ${processingCount}, ${creditsUsed}
  None of these are passed by InvestigationController.list()
  Fix: Add these to the model in the controller

BUG-04: investigation/detail.html — HTMX status polling
  hx-get="/investigations/__${investigation.id}__/status"
  Double-underscore syntax is wrong for Thymeleaf inline expressions
  Fix: Use th:attr or th:hx-get with proper Thymeleaf expression
```

---

## PART 3 — CAPABILITY SCORECARD

| Domain | Built | Partial | Missing | Score |
|---|---|---|---|---|
| Authentication | ✅ Login/Register/JWT | ⚠️ No password reset, no SSO | ❌ Magic link | 60% |
| Investigation CRUD | ✅ Create/Read/Delete/Retry | ⚠️ No edit | ❌ Batch/group | 65% |
| OSINT Intelligence | ⚠️ AI mock only | — | ❌ Real data sources | 15% |
| Results Display | ❌ Raw JSON dump | — | ❌ Structured UI | 10% |
| Confidence Scoring | ⚠️ LLM passthrough | — | ❌ Real algorithm | 20% |
| Job Processing | ✅ Queue + scheduler | ⚠️ Single-threaded | ❌ Parallel workers | 55% |
| PDF Reports | ✅ Branded, structured | ⚠️ No chain of custody | ❌ Evidence packaging | 70% |
| Credit System | ✅ Full deduct/refund | ⚠️ No top-up UI | ❌ Stripe billing | 50% |
| Admin Panel | ✅ Org settings, users | ⚠️ Bugs in template | ❌ Invite users | 55% |
| Compliance | ⚠️ Privacy notice only | — | ❌ Audit logging, retention | 20% |
| Notifications | ❌ None | — | ❌ Email on completion | 0% |
| Batch Processing | ❌ None | — | ❌ CSV upload | 0% |
| Billing | ❌ None | — | ❌ Stripe integration | 0% |
| **OVERALL** | | | | **~35%** |

---

## PART 4 — PRD VERSION 0.2

---

# AlumniBeacon PRD v0.2
## "Make It Real" — From Skeleton to Working Product

**Version:** 0.2  
**Date:** 31 March 2026  
**Status:** Engineering Handoff  
**Author:** August AI  
**Tagline:** *"Rediscovering Connections with Integrity and Purpose"*

---

### Executive Summary

Version 0.1 delivered a working skeleton: authentication, investigation management, a job queue, an AI-powered OSINT adapter, and a branded PDF report. The core architecture is sound.

Version 0.2 has one mission: **make the product actually work for a paying education centre customer**. That means:
1. Fix all template bugs
2. Display results in a human-readable, structured format
3. Implement real (or real-adjacent) OSINT data enrichment
4. Wire up billing so the product can generate revenue
5. Add the missing UX features that make it usable (search, pagination, email notifications, password reset)

This is not a feature expansion release. It is a **quality and completeness** release.

---

### Target User (v0.2 Focus)

**Sarah, Alumni Relations Manager** at a Tasmanian independent school.
- Logs in, enters a former student's name, graduation year, and last known suburb.
- Clicks "Begin OSINT Search".
- Waits 30–90 seconds.
- Sees a clean, structured results card showing: current address, phone number, email, LinkedIn profile, current employer.
- Downloads a branded PDF to share with the principal.
- Receives an email when the search completes (she stepped away).

Every feature in v0.2 serves Sarah's workflow.

---

### v0.2 Feature Scope

#### PRIORITY 0 — Bug Fixes (Ship Day 1)

**BUG-01: Fix admin template variable references**
- `${currentUser.userId}` → `${currentUserId}`
- Add `contactEmail` field to `Tenant` model and migration
- Wire `contactEmail` in `AdminController.updateSettings()`

**BUG-02: Fix investigation list controller**
Add to `InvestigationController.list()` model:
```java
model.addAttribute("totalCount", investigations.size());
model.addAttribute("completedCount", investigations.stream().filter(i -> "COMPLETED".equals(i.status())).count());
model.addAttribute("processingCount", investigations.stream().filter(i -> "PROCESSING".equals(i.status())).count());
model.addAttribute("creditsUsed", creditService.getUsed(td.tenantId()));
```

**BUG-03: Fix HTMX polling expression in detail.html**
```html
<!-- Wrong -->
hx-get="/investigations/__${investigation.id}__/status"
<!-- Correct -->
th:attr="hx-get=@{/investigations/{id}/status(id=${investigation.id})}"
```

**BUG-04: Fix investigation list search/filter**
Add `@RequestParam` handling to `InvestigationController.list()` for `search` and `status` query params.

---

#### PRIORITY 1 — Structured Results Display

**Feature: Human-Readable Investigation Results Card**

Replace the raw JSON `<pre>` dump in `investigation/detail.html` with a structured results card.

**UI Layout:**
```
┌─────────────────────────────────────────────────────────────────┐
│  INVESTIGATION RESULTS                              [Download PDF]│
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  CONTACT INFORMATION                                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  📧 Email          sarah.mitchell@gmail.com              │   │
│  │  📱 Phone          0412 345 678                          │   │
│  │  🏠 Address        42 Collins St, Hobart TAS 7000        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  PROFESSIONAL                                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  💼 Employer       ANZ Bank (2019–present)               │   │
│  │  🔗 LinkedIn       linkedin.com/in/sarah-mitchell-tas    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  SOCIAL MEDIA                                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Facebook          facebook.com/sarah.mitchell.hobart    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
│  SUMMARY                                                          │
│  Sarah Mitchell (née Thompson) appears to be living in Hobart,   │
│  Tasmania. Active LinkedIn profile confirmed. Email verified      │
│  via professional directory.                                      │
│                                                                   │
│  RECOMMENDED ACTIONS                                              │
│  • Send reconnection email to sarah.mitchell@gmail.com           │
│  • Verify address via postal mail before phone contact           │
│                                                                   │
│  DATA SOURCES                                                     │
│  • LinkedIn public profile  • Professional directory             │
│  • Electoral roll (public)  • Web search aggregation            │
│                                                                   │
│  ⚠️  PRIVACY FLAGS                                               │
│  • Address is residential — handle with care                     │
└─────────────────────────────────────────────────────────────────┘
```

**Implementation:**
- Parse `result_json` in `InvestigationDto` using Jackson
- Add typed fields: `foundEmail`, `foundPhone`, `foundAddress`, `foundEmployer`, `foundLinkedin`, `foundFacebook`, `sources`, `summary`, `recommendedActions`, `privacyFlags`
- Render each field conditionally (only show if non-null/non-empty)
- Show "Not found" placeholder for missing fields
- Colour-code privacy flags in amber

---

#### PRIORITY 2 — Real OSINT Enrichment

**Feature: Multi-Source OSINT Pipeline**

The current adapter asks an LLM to "investigate" a person. This produces hallucinated results. v0.2 replaces this with a real enrichment pipeline.

**Approach — Tiered Data Sources:**

| Tier | Source | Method | Cost | Compliance |
|---|---|---|---|---|
| 1 | **Google/Bing Search API** | Structured web search for name + location | ~$0.005/query | Public data |
| 2 | **LinkedIn via Proxycurl** | Profile lookup by name + company | ~$0.01/lookup | ToS-compliant API |
| 3 | **People Data Labs** | Person enrichment API | ~$0.10/match | Legitimate interest |
| 4 | **Hunter.io** | Email finder by name + domain | ~$0.01/search | Legitimate interest |
| 5 | **AI Synthesis** | Claude synthesises and scores results | ~$0.002/call | N/A |

**Revised OSINT Adapter Architecture:**

```python
async def enrich_person(req: SearchRequest) -> dict:
    results = {}
    
    # Tier 1: Web search (always run)
    web_results = await google_search(f"{req.name} {req.last_known_address}")
    results['web'] = web_results
    
    # Tier 2: LinkedIn (if API key configured)
    if PROXYCURL_API_KEY:
        linkedin = await proxycurl_search(req.name, req.last_known_employer)
        results['linkedin'] = linkedin
    
    # Tier 3: People Data Labs (if API key configured)
    if PDL_API_KEY:
        pdl = await pdl_enrich(req.name, req.dob, req.last_known_address)
        results['pdl'] = pdl
    
    # Tier 4: Email finder
    if HUNTER_API_KEY and req.last_known_employer:
        email = await hunter_find(req.name, req.last_known_employer)
        results['email'] = email
    
    # Tier 5: AI synthesis and confidence scoring
    synthesised = await claude_synthesise(req, results)
    return synthesised
```

**Confidence Scoring — Real Algorithm:**

```python
def calculate_confidence(findings: dict, sources_used: list) -> dict:
    score = 0
    breakdown = {}
    
    # Data points (30 points max)
    data_points = sum([
        10 if findings.get('found_email') else 0,
        8  if findings.get('found_phone') else 0,
        7  if findings.get('found_address') else 0,
        5  if findings.get('found_linkedin') else 0,
    ])
    breakdown['data_points'] = min(data_points, 30)
    
    # Cross-references (25 points max)
    # Score based on how many independent sources agree
    cross_ref = len([s for s in sources_used if s != 'ai_synthesis']) * 6
    breakdown['cross_references'] = min(cross_ref, 25)
    
    # Verification level (25 points max)
    # Email verified = 25, LinkedIn confirmed = 20, address only = 10
    if findings.get('email_verified'):
        breakdown['verification_level'] = 25
    elif findings.get('found_linkedin'):
        breakdown['verification_level'] = 20
    elif findings.get('found_address'):
        breakdown['verification_level'] = 10
    else:
        breakdown['verification_level'] = 0
    
    # Recency (10 points max)
    # Based on data freshness from sources
    breakdown['recency'] = findings.get('recency_score', 5)
    
    # Source quality (10 points max)
    quality_map = {'pdl': 10, 'linkedin': 9, 'hunter': 8, 'web': 5, 'ai_synthesis': 2}
    source_quality = max([quality_map.get(s, 0) for s in sources_used], default=0)
    breakdown['source_quality'] = source_quality
    
    total = sum(breakdown.values())
    return {'score': total, 'breakdown': breakdown, 'label': 'HIGH' if total >= 70 else 'MEDIUM' if total >= 40 else 'LOW'}
```

**Graceful Degradation:**
- If no external API keys configured → fall back to AI-only mode with clear "LOW CONFIDENCE — AI ESTIMATE ONLY" warning
- Each tier is independently optional
- Cost per investigation tracked and displayed

---

#### PRIORITY 3 — Pagination & Search (Wire Up Existing UI)

**Feature: Working Investigation List**

The UI already has pagination and search/filter controls. Wire them up.

**Controller changes:**
```java
@GetMapping("/investigations")
public String list(
    Authentication auth, Model model,
    @RequestParam(defaultValue = "") String search,
    @RequestParam(defaultValue = "") String status,
    @RequestParam(defaultValue = "0") int page
) {
    TenantDetails td = (TenantDetails) auth.getDetails();
    Page<InvestigationDto> result = investigationService
        .listByTenantPaged(td.tenantId(), search, status, page, 20);
    
    model.addAttribute("investigations", result.getContent());
    model.addAttribute("totalCount", result.getTotalElements());
    model.addAttribute("totalPages", result.getTotalPages());
    model.addAttribute("currentPage", page);
    model.addAttribute("pageSize", 20);
    model.addAttribute("completedCount", investigationService.countByStatus(td.tenantId(), "COMPLETED"));
    model.addAttribute("processingCount", investigationService.countByStatus(td.tenantId(), "PROCESSING"));
    model.addAttribute("creditsUsed", creditService.getUsed(td.tenantId()));
    return "investigation/list";
}
```

**Repository changes:**
```java
@Query("SELECT i FROM Investigation i WHERE i.tenantId = :tenantId "
     + "AND (:search = '' OR LOWER(i.subjectName) LIKE LOWER(CONCAT('%', :search, '%'))) "
     + "AND (:status = '' OR i.status = :status) "
     + "ORDER BY i.createdAt DESC")
Page<Investigation> findByTenantFiltered(
    @Param("tenantId") String tenantId,
    @Param("search") String search,
    @Param("status") String status,
    Pageable pageable
);
```

---

#### PRIORITY 4 — Email Notifications

**Feature: Investigation Completion Email**

When an investigation completes (or fails), send an email to the user who created it.

**Implementation:**
- Add `spring-boot-starter-mail` dependency
- Configure SMTP (SendGrid recommended: free tier 100 emails/day)
- Add `NotificationService` called from `JobQueueScheduler` on completion
- Email template: branded HTML with subject name, confidence score, link to view results

**Email content:**
```
Subject: Investigation Complete — Sarah Mitchell (92% confidence)

Your AlumniBeacon investigation for Sarah Mitchell has completed.

Confidence: 92% (HIGH)
Email found: sarah.mitchell@gmail.com
LinkedIn: linkedin.com/in/sarah-mitchell-tas

[View Full Results] [Download PDF Report]

This search was conducted in compliance with the Australian Privacy Act 1988.
```

---

#### PRIORITY 5 — Password Reset

**Feature: Forgot Password Flow**

**Endpoints:**
- `GET /auth/forgot-password` — form to enter email
- `POST /auth/forgot-password` — sends reset email with token
- `GET /auth/reset-password?token={token}` — form to enter new password
- `POST /auth/reset-password` — validates token, updates password

**Implementation:**
- Add `password_reset_tokens` table (token, user_id, expires_at, used)
- Token: UUID, 1-hour expiry, single-use
- Email: SendGrid (same integration as notifications)

---

#### PRIORITY 6 — Stripe Billing

**Feature: Credit Top-Up via Stripe**

**Credit Packages:**
| Package | Credits | Price | Per Credit |
|---|---|---|---|
| Starter Pack | 50 credits | $49 | $0.98 |
| Standard Pack | 200 credits | $149 | $0.75 |
| Professional Pack | 500 credits | $299 | $0.60 |
| Enterprise Pack | 2,000 credits | $999 | $0.50 |

**Implementation:**
- Add `stripe-java` dependency
- `GET /billing` — billing page showing current balance and packages
- `POST /billing/checkout` — creates Stripe Checkout Session
- `GET /billing/success` — post-payment success page, credits added
- `POST /billing/webhook` — Stripe webhook handler (payment_intent.succeeded)
- Store Stripe customer ID on Tenant (field already exists)

**Subscription Option (future):**
Monthly subscription tiers (Starter $99/mo = 100 credits, Professional $399/mo = 500 credits) can be added in v0.3.

---

#### PRIORITY 7 — User Invite System

**Feature: Invite Team Members**

**Endpoints:**
- `POST /admin/users/invite` — sends invite email with magic link
- `GET /auth/accept-invite?token={token}` — invite acceptance form
- `POST /auth/accept-invite` — creates user account

**Implementation:**
- Add `user_invites` table (token, tenant_id, email, role, expires_at, accepted_at)
- Token: UUID, 7-day expiry
- Invited user sets their own password on acceptance
- Role selectable by admin (INVESTIGATOR or VIEWER)

---

#### PRIORITY 8 — Audit Logging (Wire Up Existing Table)

**Feature: Compliance Audit Trail**

The `audit_logs` table already exists in the schema. Wire it up.

**Events to log:**
| Event | Details |
|---|---|
| `USER_LOGIN` | user_id, ip_address, timestamp |
| `USER_LOGOUT` | user_id, timestamp |
| `INVESTIGATION_CREATED` | investigation_id, subject_name, user_id |
| `INVESTIGATION_VIEWED` | investigation_id, user_id |
| `INVESTIGATION_DELETED` | investigation_id, user_id |
| `REPORT_DOWNLOADED` | investigation_id, user_id |
| `USER_INVITED` | invited_email, role, inviting_user_id |
| `USER_REMOVED` | removed_user_id, removing_user_id |
| `CREDITS_PURCHASED` | amount, credits_added, stripe_payment_id |

**Implementation:**
- `AuditService` with `log(tenantId, userId, action, resourceType, resourceId, details, ipAddress)`
- Inject into controllers and call at each event
- `GET /admin/audit` — paginated audit log view (admin only)

---

### v0.2 Database Changes

```sql
-- V2__v0.2_additions.sql

-- Add contactEmail to tenants
ALTER TABLE tenants ADD COLUMN contact_email TEXT;

-- Password reset tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- User invites
CREATE TABLE IF NOT EXISTS user_invites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id TEXT NOT NULL,
    invited_by TEXT NOT NULL,
    email TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'INVESTIGATOR',
    token TEXT NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    accepted_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (invited_by) REFERENCES users(id)
);

-- Stripe billing events
CREATE TABLE IF NOT EXISTS billing_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tenant_id TEXT NOT NULL,
    stripe_event_id TEXT UNIQUE,
    event_type TEXT NOT NULL,
    credits_added INTEGER DEFAULT 0,
    amount_cents INTEGER DEFAULT 0,
    currency TEXT DEFAULT 'aud',
    metadata_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_user_invites_token ON user_invites(token);
CREATE INDEX IF NOT EXISTS idx_billing_events_tenant ON billing_events(tenant_id);
```

---

### v0.2 Non-Functional Requirements

| NFR | Requirement | Implementation |
|---|---|---|
| **Security** | Fix missing SecurityConfig.java | Recreate with proper route security |
| **Security** | Rate limiting on auth endpoints | Spring Security or Bucket4j (10 req/min on /auth/*) |
| **Privacy** | Audit all data access | AuditService wired to all controllers |
| **Privacy** | Data retention notice | Display 90-day retention policy in UI |
| **Performance** | Parallel job processing | Increase scheduler pool to 3 workers |
| **Reliability** | Health check includes adapter | `/actuator/health` checks OSINT adapter |
| **Observability** | Request logging | Log all investigation creates with tenant_id |
| **Database** | Migration to PostgreSQL | Optional for v0.2, required for v0.3 |

---

### v0.2 Implementation Roadmap

| Week | Deliverables | Owner |
|---|---|---|
| **Week 1** | BUG-01 to BUG-04 fixes, SecurityConfig.java, structured results display | Backend + Frontend |
| **Week 2** | Real OSINT enrichment (Google Search + Proxycurl), real confidence scoring | Backend + OSINT |
| **Week 3** | Pagination/search wired, email notifications, password reset | Backend + Frontend |
| **Week 4** | Stripe billing (credit top-up), user invite system, audit logging | Backend |
| **Week 5** | Testing, bug fixes, staging deployment, pilot customer onboarding | QA + DevOps |

---

### v0.2 Success Criteria

The release is complete when:

1. ✅ A user can register, log in, and reset their password
2. ✅ A user can create an investigation and see structured results (not raw JSON)
3. ✅ Results include at minimum: address, phone, email, LinkedIn (when found)
4. ✅ Confidence score reflects real data quality, not LLM guess
5. ✅ User receives email when investigation completes
6. ✅ Investigation list is searchable and paginated
7. ✅ Admin can invite team members
8. ✅ Customer can purchase credits via Stripe
9. ✅ All data access is written to audit log
10. ✅ No template bugs (all model attributes correctly wired)

---

### v0.2 Out of Scope (Deferred to v0.3)

- Batch / CSV upload
- Alumni self-claim portal
- CRM integrations (Salesforce, HubSpot, Blackbaud)
- REST API for external integrations
- SUPER_ADMIN platform management portal
- Mobile-responsive layout
- Scheduled/recurring searches
- PostgreSQL migration
- WebSocket (HTMX polling is sufficient)
- Benchmark reports

---

### Risk Register (v0.2)

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Real OSINT APIs return poor results for Australian names | HIGH | HIGH | Test with People Data Labs AU dataset before committing; have AI fallback with clear LOW confidence label |
| Stripe integration complexity delays billing | MEDIUM | HIGH | Use Stripe Checkout (hosted page) — minimal code, fast to ship |
| Email deliverability (spam filters) | MEDIUM | MEDIUM | Use SendGrid with proper SPF/DKIM; transactional email only |
| SQLite write contention under load | MEDIUM | MEDIUM | Limit concurrent jobs to 1 (already the case); plan PostgreSQL migration for v0.3 |
| Privacy Act compliance for real data sources | MEDIUM | HIGH | Ensure all API providers have legitimate interest basis; add consent notice to investigation form |

---

### Appendix — Current Tech Stack (v0.1 Actual)

| Layer | Technology | Version |
|---|---|---|
| Backend | Spring Boot | 3.4.1 |
| Language | Java | 21 |
| Frontend | Thymeleaf + HTMX | SSR |
| Styling | Tailwind CSS | CDN |
| Database | SQLite | 3.47.1 |
| ORM | Hibernate / Spring Data JPA | 6.6 |
| Migrations | Flyway | 10.21 |
| Auth | JJWT | 0.12.6 |
| PDF | iText | 8.0.5 |
| HTTP Client | Spring WebFlux WebClient | 3.4.1 |
| OSINT Adapter | Python FastAPI | 0.115.6 |
| AI Model | Claude claude-sonnet-4-20250514 via OpenRouter | — |
| Deployment | Docker + Render | — |

---

**END OF DOCUMENT**

*PRD v0.2 — AlumniBeacon — 31 March 2026*  
*Next review: After Week 5 delivery — PRD v0.3 to cover batch processing, self-claim portal, and CRM integrations.*
