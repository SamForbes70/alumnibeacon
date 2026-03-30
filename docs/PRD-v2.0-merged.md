# AlumniBeacon - Product Requirements Document (PRD)
## (Formerly Lost Alumni Finder)

**Version:** 2.0  
**Date:** March 30, 2026  
**Status:** Ready for Stakeholder Review  
**Author:** August AI Assistant + Grok Solution Architect  

**Tagline:** *"Rediscovering Connections with Integrity and Purpose"*

---

## 1. Executive Summary

### 1.1 Product Vision
AlumniBeacon is a **secure, multi-tenant SaaS platform** purpose-built for schools, universities, and alumni associations to locate "lost" alumni and restore community connections. The platform combines **ethical AI-driven OSINT** (Open Source Intelligence) with a **5-stage investigation workflow**, delivering confidence-scored results while maintaining strict Australian Privacy Act 1988 compliance.

The platform embodies **calm, prestigious, values-led aesthetics** inspired by Tasmanian Anglican heritage-school traditions—honouring tradition, belonging, and service while respecting privacy and dignity.

### 1.2 Target Market
- **Primary:** Private schools, universities, and alumni associations (Australia/NZ initially)
- **Secondary:** Professional associations, non-profits, corporate alumni networks
- **Expansion:** Global education markets with GDPR compliance

### 1.3 Value Proposition
- **Ethical Investigation:** "Aggressive but ethical" scanning via compliant APIs and public sources only
- **AI Confidence Scoring:** Multi-factor algorithm (0-100%) with traffic-light system
- **Compliance-First:** Australian Privacy Act 1988 / APPs fully supported, GDPR-ready
- **Self-Claim Portal:** Found alumni can verify/update details and opt-in/out
- **Professional Reports:** Court-ready evidence packages with chain of custody
- **CRM Integration:** Seamless sync with Salesforce, HubSpot, Blackbaud

### 1.4 Business Model
- **Pricing:** Tiered subscription ($99 - $2,000/month) + pay-per-credit model
- **Revenue Target:** $15k - $40k MRR within 12 months
- **Sales Cycle:** 30-60 days (B2B institutional sales)
- **Differentiation:** AI-driven enrichment, multi-tenant isolation, education-specific compliance

---

## 2. Branding & UX Guidelines

### 2.1 Brand Identity
**Core Values Evoked:** Character, wellbeing, learning, service, outdoor challenge, belonging, tradition, integrity, humility, kindness, courage, respect.

**Visual Language:** Calm, prestigious, values-led, community-centred. Generous white space, editorial hierarchy, elegant typography, heritage cues (subtle arches, light rays, river-stone motifs), modern premium finish.

### 2.2 Colour Palette

| Token | Hex | Usage |
|-------|-----|-------|
| **Primary (Deep Charcoal)** | `#1C2526` | Headers, primary backgrounds, logo |
| **Accent (Magenta)** | `#9C2A6B` | CTAs, highlights, badges |
| **Highlight (Muted Gold)** | `#C9A66B` | Success states, premium accents |
| **Background (Warm White)** | `#F8F1E9` | Page backgrounds, cards |
| **Secondary (Slate)** | `#475569` | Body text, secondary elements |
| **Success (Deep Green)** | `#2E5F4A` | HIGH confidence, success states |
| **Warning (Amber)** | `#D97706` | MEDIUM confidence, alerts |
| **Error (Crimson)** | `#DC2626` | LOW confidence, errors |
| **Environmental (River Blue)** | `#4A7C9D` | Optional accents, charts |

### 2.3 Typography
- **Headings:** Playfair Display or "Beacon Serif" (elegant serif, 600-700 weight)
- **Body:** Inter or system sans-serif (400 weight, maximum readability)
- **Monospace:** JetBrains Mono (logs, timestamps, technical data)

### 2.4 Logo Concept
A minimalist **lighthouse beacon** whose light rays subtly form an open bridge arch, rendered in muted gold on deep charcoal. The word "AlumniBeacon" sits beneath in refined serif lettering. Symbolises guidance, heritage, and reconnection without any school-specific iconography.

### 2.5 UX Principles
- **Mobile-first** with generous whitespace
- **Soft shadows** and purposeful micro-animations (never energetic/noisy)
- **Dark/light mode** with charcoal base
- **Wizard-style flows** for data upload and search initiation
- **Accessibility:** WCAG 2.2 AA compliant

### 2.6 Imagery Style
Real, natural, candid photography: students learning outdoors, exploring Tasmanian landscapes, performing, competing, engaging in community service. Warm, purposeful, never staged or "startup flashy".

---

## 3. Product Objectives

1. Enable organisations to locate lost alumni quickly, ethically, and at scale
2. Provide 100% data isolation and privacy-first design (row-level security)
3. Deliver elegant, low-friction UX that feels premium and heritage-inspired
4. Generate actionable, auditable reports for governance and impact measurement
5. Achieve sustainable revenue while covering data-API costs
6. Differentiate through AI confidence scoring, consent workflows, and heritage branding

---

## 4. Target Users & Personas

### 4.1 User Roles

| Role | Permissions | Use Case |
|------|-------------|----------|
| **Super Admin** (Platform Owner) | Full tenant management, billing, feature flags, global compliance | Platform operations |
| **Organisation Admin** (Alumni Officer) | Upload data, trigger searches, review reports, manage users | Daily operations |
| **Investigator** | Create investigations, view results, download reports | Research staff |
| **Viewer** | Read-only access to investigations and reports | Management |
| **API User** | Programmatic access via scoped API keys | System integrations |

### 4.2 Personas

**Sarah (45)** - Alumni Relations Manager, Tasmanian independent school
- Time-poor, values compliance and ease
- Needs quick wins to show board value
- Concerned about privacy regulations

**Michael (52)** - University Advancement Director
- Needs bulk processing and exportable reports
- Manages team of 5 researchers
- Requires CRM integration for existing workflows

---

## 5. Functional Requirements

### 5.1 Authentication & Multi-Tenancy
- **Super-admin portal** (platform owners only) – full access to tenants, billing, feature flags
- **Organisation-specific logins** – email + password + magic link + SSO (Google/Microsoft)
- **Strict data isolation:** Every record tagged with `tenant_id`; database-level row-level security (RLS)
- **JWT tokens:** Access tokens (15 min expiry), refresh tokens (7 days)
- **Password Policy:** Min 12 chars, uppercase, lowercase, number, special char

### 5.2 Lost-Alumni Input Methods

| Method | Description | Priority |
|--------|-------------|----------|
| **Manual Entry** | Single-entry form (name, DOB, last known address, graduation year, notes) | P0 |
| **Bulk CSV Upload** | Template download with validation; 1,000 records per batch | P0 |
| **Excel Import** | .xlsx support with multiple sheets | P1 |
| **Integration Import** | Google Sheets, Salesforce, Blackbaud API | P2 |

**Upload Validation:**
- Required: Full name OR email OR phone (minimum one identifier)
- Optional: DOB, last known location, graduation years, notes
- Auto-formatting: Name capitalisation, phone normalisation
- Error reporting: Row-by-row validation feedback

### 5.3 Search Engine (Ethical OSINT)

#### "Aggressive but Ethical" Approach
All scanning is **rate-limited, API-first, and legally compliant**—no Terms of Service violations, no unauthorised access.

**Data Sources (Prioritised):**

| Source Type | Examples | Compliance |
|-------------|----------|------------|
| **Commercial APIs** | People Data Labs, Apollo.io, Global Data CASPAR, Proxycurl LinkedIn | Legitimate interest, opt-out honoured |
| **Public Records** | Electoral rolls (where legally accessible), property records | Public domain |
| **Professional Networks** | LinkedIn (via API), company directories | Terms-compliant scraping |
| **OSINT Web Search** | AI summarisation of public web (rate-limited) | Fair use, robots.txt respected |

**Search Features:**
- **One-click Search:** Immediate initiation for single records
- **Scheduled Batch Jobs:** Off-peak processing for bulk lists
- **AI Fuzzy Matching:** Handles name variations, typos, married names
- **Confidence Scoring:** 0-100% score with breakdown (see Section 5.5)
- **Manual Review Queue:** Human approval required before marking "found"

### 5.4 Investigation Workflow (5-Stage)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  INITIATION │───▶│ INVESTIGATION│───▶│   ANALYSIS  │───▶│     QC      │───▶│   DELIVERY  │
│   (Queue)   │    │  (Running)   │    │ (Processing)│    │(Verification)│    │  (Complete) │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
      │                   │                   │                   │                   │
      ▼                   ▼                   ▼                   ▼                   ▼
   • Validate          • Corporate         • Cross-reference   • False positive   • Generate
     inputs              intelligence        data sources      detection          PDF report
   • Queue job         • Social media      • Pattern         • Confidence       • Package
   • Assign ID           scraping            recognition         scoring            evidence
   • Log start         • Public records    • Entity          • Quality          • Notify
                         search              resolution        assurance          completion
```

#### Stage 1: Initiation
- **Trigger:** User submits investigation request
- **Actions:**
  - Validate required fields (minimum one identifier)
  - Generate unique investigation ID (UUID)
  - Queue job in Redis (priority-aware)
  - Create initial database record (status: PENDING)
- **Output:** Investigation queued confirmation

#### Stage 2: Investigation
- **Trigger:** Job picked up by worker
- **Actions:**
  - Query commercial enrichment APIs (rate-limited)
  - Search public records (where legal)
  - Scrape professional networks (API-compliant)
  - AI-powered web search summarisation
- **Output:** Raw data collection, status: RUNNING

#### Stage 3: Analysis
- **Trigger:** Data collection complete
- **Actions:**
  - Cross-reference multiple data sources
  - Pattern recognition (naming variations, location history)
  - Entity resolution (consolidate fragmented data)
  - Timeline reconstruction
- **Output:** Structured findings, status: ANALYZING

#### Stage 4: Quality Control
- **Trigger:** Analysis complete
- **Actions:**
  - False positive detection (common name filtering)
  - Confidence scoring algorithm (see 5.5)
  - Cross-reference verification
  - Data freshness assessment
- **Output:** Confidence score, status: QC

#### Stage 5: Delivery
- **Trigger:** QC passed
- **Actions:**
  - Generate professional PDF report
  - Package evidence with chain of custody
  - Update investigation record (status: COMPLETED)
  - Notify user via email + dashboard
- **Output:** Downloadable report, status: COMPLETED

### 5.5 Confidence Scoring Algorithm

**Multi-Factor Scoring (0-100%):**

| Factor | Weight | Description |
|--------|--------|-------------|
| **Data Points** | 30% | Number of corroborating data sources |
| **Cross-References** | 25% | Consistency across multiple platforms |
| **Verification Level** | 25% | Strength of identity verification |
| **Recency** | 10% | Freshness of data (0-12 months optimal) |
| **Source Quality** | 10% | Reliability of data sources (A-F grading) |

**Traffic Light System:**
- **HIGH (≥85%):** Strong confidence, actionable intelligence (Deep Green #2E5F4A)
- **MEDIUM (60-84%):** Moderate confidence, manual review recommended (Amber #D97706)
- **LOW (<60%):** Weak confidence, likely false positive (Crimson #DC2626)

**Transparency:** Users see full breakdown per investigation, not just final score.

### 5.6 Alumni Self-Claim Portal

**Purpose:** Build consent & trust; reduce false positives

**Features:**
- Found alumni receive email invitation (optional)
- Secure login via magic link
- Verify/update contact details
- Opt-in/out of future contact
- View what data the organisation holds
- Request deletion (GDPR/Privacy Act compliance)

**Priority:** HIGH - Builds trust, improves data quality

### 5.7 Activity & Compliance Reports

**Comprehensive PDF/CSV Export:**

| Report Type | Contents | Audience |
|-------------|----------|----------|
| **Activity Report** | Search history, records processed, match rates, time-to-find | Operations |
| **Privacy Audit Log** | Who accessed what, when; consent status summary | Compliance officers |
| **ROI Dashboard** | Success trends, cost per match, fundraising attribution | Executives |
| **Benchmark Report** | Anonymised success rates vs. similar organisations | Strategy |

**Dashboard Analytics:**
- Real-time metrics (investigations today, success rate)
- Trend charts (weekly/monthly/quarterly)
- Cost tracking (API spend per investigation)

### 5.8 Admin Features (Super Admin)

- **Tenant Management:** Create, suspend, delete organisations
- **Usage Quotas:** Set monthly investigation limits per tenant
- **Billing Overrides:** Custom pricing, grace periods
- **Global Search Tuning:** Adjust confidence thresholds, API key rotation
- **Feature Flags:** Phased rollouts, beta features

---

## 6. Non-Functional Requirements

### 6.1 Security
- **Encryption at Rest:** AES-256 for database and S3 storage
- **Encryption in Transit:** TLS 1.3 for all connections
- **Authentication:** JWT with secure httpOnly cookies
- **Audit Logging:** All data access logged with user ID, timestamp, IP
- **SOC 2 Type II:** Target certification within 12 months of launch

### 6.2 Privacy & Compliance

**Australian Privacy Act 1988 / APPs:**
- Data minimisation (only collect what's necessary)
- Legitimate interest notices (clear disclosure of purpose)
- Right-to-be-forgotten (auto-delete after 90 days, immediate on request)
- Consent management (opt-in/opt-out workflows)
- Sample privacy notices and data-processing addendums provided

**GDPR (EU Alumni):**
- Explicit consent capture
- Data portability (export in machine-readable format)
- 72-hour breach notification
- Privacy by design (default minimal data collection)

**PII Handling:**
- SHA-256 hashing for sensitive identifiers
- Spacy-based named entity recognition for redaction
- Automatic redaction in logs and reports

### 6.3 Performance & Scalability

| Metric | Target | Implementation |
|--------|--------|----------------|
| **Single Record Search** | <60 seconds | Async background jobs |
| **Bulk Processing (1,000)** | <1 hour | Parallel workers, queue sharding |
| **API Response Time (p95)** | <200ms | Connection pooling, Redis caching |
| **WebSocket Latency** | <100ms | Direct Redis pub/sub |
| **Database Query (p95)** | <50ms | Proper indexing, query optimisation |
| **Uptime** | 99.9% | Multi-AZ deployment, auto-scaling |

**Scalability:** Cloud-native (AWS/GCP), auto-scaling based on queue depth

---

## 7. Technical Architecture

### 7.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      CLIENT LAYER                                        │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────────────────┐  │
│  │   Next.js 15 Frontend           │  │   API Consumers (Integrations)              │  │
│  │   • React 18 + TypeScript       │  │   • Webhooks                                │  │
│  │   • Tailwind CSS + shadcn/ui    │  │   • CRM Sync (Salesforce/HubSpot)           │  │
│  │   • Real-time WebSocket         │  │   • Mobile apps (future)                    │  │
│  └─────────────────────────────────┘  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                           │
                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      API LAYER                                           │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │   FastAPI Backend (Python 3.12+)                                                │    │
│  │   • RESTful API (/api/v1/)                                                      │    │
│  │   • WebSocket Server (/ws/)                                                     │    │
│  │   • JWT Authentication                                                          │    │
│  │   • Pydantic v2 Validation                                                      │    │
│  │   • Async Request Handling                                                      │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    ▼                      ▼                      ▼
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│      DATA LAYER         │  │      JOB QUEUE          │  │      STORAGE            │
│  ┌─────────────────┐    │  │  ┌─────────────────┐    │  │  ┌─────────────────┐    │
│  │  PostgreSQL 15+ │    │  │  │  Redis 7+       │    │  │  │  AWS S3         │    │
│  │  • Asyncpg      │    │  │  │  • BullMQ       │    │  │  │  • Reports      │    │
│  │  • Row-Level    │    │  │  │  • Pub/Sub      │    │  │  │  • Evidence     │    │
│  │    Security     │    │  │  │  • Session      │    │  │  │  • CSV Uploads  │    │
│  │  • JSONB for    │    │  │  │    Cache        │    │  │  │                 │    │
│  │    findings     │    │  │  └─────────────────┘    │  │  └─────────────────┘    │
│  └─────────────────┘    │  └─────────────────────────┘  └─────────────────────────┘
└─────────────────────────┘
```

### 7.2 Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| **Frontend** | Next.js | 15.x | React framework with App Router |
| | TypeScript | 5.0+ | Type safety |
| | Tailwind CSS | 3.4+ | Utility-first styling |
| | shadcn/ui | Latest | Heritage-inspired accessible components |
| | Lucide React | Latest | Icon library |
| **Backend** | FastAPI | 0.100+ | Async Python web framework |
| | Pydantic | 2.0+ | Data validation |
| | Uvicorn | 0.23+ | ASGI server |
| | Python | 3.12+ | Runtime |
| **Database** | PostgreSQL | 15+ | Primary data store with RLS |
| | Asyncpg | 0.28+ | Async PostgreSQL driver |
| | Redis | 7+ | Job queue & caching |
| **Queue** | BullMQ | Latest | Redis-based job queue |
| **Storage** | AWS S3 | - | Report & evidence storage |
| **Auth** | python-jose | 3.3+ | JWT tokens |
| | passlib | 1.7+ | Password hashing |
| **AI** | OpenAI/Anthropic | Latest | Fuzzy matching, summarisation |
| **Email** | SendGrid/Mailgun | - | Transactional & outreach |

### 7.3 Database Schema

```sql
-- Organizations (Tenants)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    subscription_tier VARCHAR(50) DEFAULT 'starter', -- starter, professional, enterprise
    subscription_status VARCHAR(50) DEFAULT 'active',
    max_investigations_per_month INTEGER DEFAULT 100,
    max_users INTEGER DEFAULT 5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Users (with RLS)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'investigator', -- super_admin, admin, investigator, viewer
    organization_id UUID REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users 
    USING (organization_id = current_setting('app.current_tenant')::UUID);

-- Investigations
CREATE TABLE investigations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id),
    subject_name VARCHAR(255) NOT NULL,
    subject_dob DATE,
    subject_last_known_location VARCHAR(255),
    subject_school_years VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, QUEUED, RUNNING, COMPLETED, FAILED
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    confidence_score DECIMAL(5,2),
    confidence_breakdown JSONB,
    progress_percent INTEGER DEFAULT 0,
    current_stage VARCHAR(50),
    findings JSONB,
    error_message TEXT,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS on investigations
ALTER TABLE investigations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON investigations 
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- Job logs
CREATE TABLE job_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id),
    investigation_id UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    level VARCHAR(20) NOT NULL, -- INFO, WARN, ERROR
    message TEXT NOT NULL,
    stage VARCHAR(50),
    metadata JSONB
);

-- Enable RLS on job_logs
ALTER TABLE job_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON job_logs 
    USING (tenant_id = current_setting('app.current_tenant')::UUID);

-- Alumni self-claims
CREATE TABLE alumni_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id),
    investigation_id UUID REFERENCES investigations(id),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    verified_data JSONB,
    consent_status VARCHAR(50) DEFAULT 'pending', -- pending, opted_in, opted_out
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_investigations_tenant ON investigations(tenant_id);
CREATE INDEX idx_investigations_status ON investigations(status);
CREATE INDEX idx_investigations_created_at ON investigations(created_at);
CREATE INDEX idx_job_logs_investigation ON job_logs(investigation_id);
CREATE INDEX idx_job_logs_timestamp ON job_logs(timestamp);
CREATE INDEX idx_users_org ON users(organization_id);
```

### 7.4 API Specifications

#### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | JWT token acquisition |
| POST | `/api/v1/auth/refresh` | Token refresh |
| POST | `/api/v1/auth/logout` | Token revocation |
| POST | `/api/v1/auth/magic-link` | Send magic link email |

#### Investigations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/investigations` | List all (paginated, filtered) |
| POST | `/api/v1/investigations` | Create new investigation |
| GET | `/api/v1/investigations/{id}` | Get details |
| PATCH | `/api/v1/investigations/{id}` | Update |
| DELETE | `/api/v1/investigations/{id}` | Delete |
| POST | `/api/v1/investigations/{id}/rerun` | Re-run |
| POST | `/api/v1/investigations/bulk` | Bulk create from CSV |

#### Jobs & Progress
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/investigations/{id}/jobs` | Job status and logs |
| GET | `/api/v1/investigations/{id}/reports/download` | Download PDF |

#### Alumni Self-Claim
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/claims/{token}/verify` | Verify claim token |
| POST | `/api/v1/claims/{token}/update` | Update details |
| POST | `/api/v1/claims/{token}/consent` | Set consent preference |

#### WebSocket
| Endpoint | Description |
|----------|-------------|
| `/ws/investigations/{id}` | Real-time progress updates |

### 7.5 Pydantic Models

```python
from pydantic import BaseModel, Field
from datetime import datetime
from typing import Optional, List
from enum import Enum

class InvestigationStatus(str, Enum):
    PENDING = "PENDING"
    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"

class Priority(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class InvestigationCreate(BaseModel):
    subject_name: str = Field(..., min_length=2, max_length=255)
    subject_dob: Optional[str] = None
    subject_last_known_location: Optional[str] = None
    subject_school_years: Optional[str] = None
    priority: Priority = Priority.MEDIUM
    notes: Optional[str] = None

class InvestigationFindings(BaseModel):
    locations: List[dict] = []
    employment: List[dict] = []
    social_profiles: List[dict] = []
    contact_info: Optional[dict] = None
    family_connections: List[dict] = []
    education_history: List[dict] = []

class ConfidenceBreakdown(BaseModel):
    data_points: int = Field(..., ge=0, le=100)
    cross_references: int = Field(..., ge=0, le=100)
    verification_level: str  # HIGH, MEDIUM, LOW
    recency_score: int = Field(..., ge=0, le=100)
    source_quality: str  # A, B, C, D, F
    overall_score: float = Field(..., ge=0, le=100)

class InvestigationResponse(BaseModel):
    id: str
    tenant_id: str
    subject_name: str
    subject_dob: Optional[str]
    subject_last_known_location: Optional[str]
    subject_school_years: Optional[str]
    status: InvestigationStatus
    priority: Priority
    confidence_score: Optional[float]
    confidence_breakdown: Optional[ConfidenceBreakdown]
    progress_percent: int
    current_stage: Optional[str]
    findings: Optional[InvestigationFindings]
    created_at: datetime
    started_at: Optional[datetime]
    completed_at: Optional[datetime]
    error_message: Optional[str]

class JobLog(BaseModel):
    timestamp: datetime
    level: str  # INFO, WARN, ERROR
    message: str
    stage: Optional[str]
    metadata: Optional[dict]

class InvestigationJob(BaseModel):
    id: str
    investigation_id: str
    status: InvestigationStatus
    progress_percent: int
    current_stage: Optional[str]
    logs: List[JobLog]
    error_message: Optional[str]
    started_at: Optional[datetime]
    completed_at: Optional[datetime]
```

---

## 8. UI/UX Design Specifications

### 8.1 Key UI Components

#### InvestigationRow (Dashboard List Item)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                                                                     │
│  ┌──────┐  Dale Bigham                                    [COMPLETED]  [HIGH]  [⋯] │
│  │  DB  │  Class of 1986 • Melbourne                            92% confidence     │
│  └──────┘                                                      Completed 2h ago    │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**Color Coding:**
- **PENDING:** Slate-400 badge, Clock icon, 0% progress
- **RUNNING:** Magenta (#9C2A6B) badge, animated spinner, live progress bar
- **COMPLETED (High):** Deep green (#2E5F4A) badge, CheckCircle icon, gold score
- **COMPLETED (Medium):** Amber badge, CheckCircle icon
- **COMPLETED (Low):** Crimson badge, Alert icon
- **FAILED:** Crimson badge, XCircle icon

#### Investigation Detail Page Layout

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│  AlumniBeacon                                                        [User ▼] [⚙️] │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  Dale Bigham Investigation                              [Re-run] [Download ▼] [Edit]│
│  ═════════════════════════                                                          │
│                                                                                     │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────────────┐   │
│  │  STATUS                         │  │  FINDINGS                               │   │
│  │  ┌─────────────────────────┐   │  │                                         │   │
│  │  │    [✓] COMPLETED        │   │  │  • Location: Melbourne, VIC            │   │
│  │  │                         │   │  │  • Employment: BHP (2019-present)      │   │
│  │  │  Confidence: 92%        │   │  │  • LinkedIn: linkedin.com/in/...       │   │
│  │  │  Duration: 20 minutes   │   │  │  • Contact: dale.bigham@email.com      │   │
│  │  │                         │   │  │                                         │   │
│  │  │  [████████░░] 92%       │   │  │  Confidence Breakdown:                  │   │
│  │  └─────────────────────────┘   │  │  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━    │   │
│  │                                │  │  Data Points      ████████████░░░  80% │   │
│  │  [Overview] [Logs] [Evidence]  │  │  Cross-References ██████████░░░░░  75% │   │
│  │                                │  │  Verification     █████████████░░  90% │   │
│  └─────────────────────────────────┘  │  Recency          ██████████████░  95% │   │
│                                       │  Source Quality   ████████████░░░  85% │   │
│                                       │                                         │   │
│                                       └─────────────────────────────────────────┘   │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

#### Create Investigation Modal

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  Start New Investigation                                       [×]  │
│  ═════════════════════════════════                                  │
│                                                                     │
│  Subject Name *                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Dale Bigham                                                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Date of Birth                                                      │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 1970-03-15                                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Last Known Location                                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ Melbourne, Victoria                                          │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  School Years                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 1982-1986                                                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Priority                        [Medium ▼]                         │
│                                                                     │
│                                                                     │
│                        [Cancel]      [Start Investigation]          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Responsive Breakpoints

| Breakpoint | Width | Adjustments |
|------------|-------|-------------|
| Mobile | < 640px | Single column, hamburger nav, stacked cards |
| Tablet | 640-1024px | Two-column grid, condensed sidebar |
| Desktop | > 1024px | Full layout, expanded sidebar, data tables |

---

## 9. Additional Value-Add Features

| Feature | Value | Priority | Status |
|---------|-------|----------|--------|
| **Alumni Self-Claim Portal** | Builds consent & trust; reduces false positives | HIGH | MVP |
| **CRM Sync** (Salesforce/HubSpot/Blackbaud) | Seamless workflow integration | HIGH | V1.1 |
| **AI Confidence + Human Review** | Improves accuracy & compliance | HIGH | MVP |
| **Consent & Outreach Templates** | Turns finds into relationships | MEDIUM | V1.1 |
| **Benchmark Reports** | Competitive insight | MEDIUM | V1.1 |
| **Batch Scheduling** | Operational ease | HIGH | MVP |
| **API Access** | Institutional integrations | MEDIUM | V2 |

---

## 10. Pricing & Monetization

### 10.1 Subscription Tiers

| Feature | Starter | Professional | Enterprise |
|---------|---------|--------------|------------|
| **Price** | $99/mo | $399/mo | Custom |
| **Annual Discount** | 20% off | 20% off | Custom |
| **Investigations/Month** | 100 | 500 | Unlimited |
| **Users** | 3 | 10 | Unlimited |
| **Self-Claim Portal** | ✅ | ✅ | ✅ |
| **CRM Sync** | ❌ | ✅ | ✅ |
| **API Access** | ❌ | ✅ | ✅ |
| **Batch Processing** | ❌ | ✅ | ✅ |
| **Benchmark Reports** | ❌ | ✅ | ✅ |
| **Priority Support** | Email | Email + Chat | Dedicated |
| **Custom Integration** | ❌ | ❌ | ✅ |
| **SLA** | 99.5% | 99.9% | 99.99% |

### 10.2 Additional Credits
- **Pay-as-you-go:** $0.50 per investigation (volume discounts: 1,000+ @ $0.40, 10,000+ @ $0.30)
- **overage protection:** Automatic billing, monthly caps configurable

### 10.3 Charge Page Design

**Layout:**
- Hero: "Choose the plan that honours your community" (generous whitespace, warm white background)
- Monthly/Annual toggle (save 20%)
- Three-column comparison with feature matrix
- "Most popular" badge on Professional (magenta accent)
- Trust signals: "Australian Privacy Act compliant", "Secure Stripe payments", 30-day money-back guarantee

**Stripe Integration:**
- Stripe Checkout Sessions for subscription creation
- Customer Portal for plan management
- Webhooks for fulfilment
- Stripe as source of truth for subscriptions

---

## 11. Implementation Roadmap

### 11.1 Phase 1: MVP (Weeks 1-4)
**Goal:** Core functionality for 3 pilot customers

| Week | Deliverables |
|------|-------------|
| 1 | Database schema with RLS, FastAPI skeleton, auth system |
| 2 | Investigation CRUD, job queue (BullMQ/Redis), basic frontend |
| 3 | 5-stage workflow, WebSocket progress, confidence scoring |
| 4 | PDF reports, self-claim portal, Stripe integration, testing |

### 11.2 Phase 2: Beta (Weeks 5-8)
**Goal:** Pilot customer onboarding, CRM integrations

| Week | Deliverables |
|------|-------------|
| 5 | Salesforce/HubSpot sync, advanced reports |
| 6 | Benchmark reports, batch scheduling, email notifications |
| 7 | API access, webhook system, documentation |
| 8 | Pilot feedback iteration, performance optimisation |

### 11.3 Phase 3: Launch (Weeks 9-12)
**Goal:** Production-ready, public launch

| Week | Deliverables |
|------|-------------|
| 9 | Security audit, SOC 2 preparation |
| 10 | Marketing website, case studies |
| 11 | Sales materials, demo environment |
| 12 | Public launch, PR campaign |

---

## 12. Success Metrics

### 12.1 Product Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Investigation Success Rate** | >85% | % completed with HIGH confidence |
| **Average Investigation Time** | <30 min | Time from start to completion |
| **False Positive Rate** | <10% | % LOW confidence results |
| **Self-Claim Conversion** | >30% | % of found alumni who verify details |
| **User Satisfaction (NPS)** | >50 | Quarterly surveys |

### 12.2 Business Metrics

| Metric | 6-Month | 12-Month |
|--------|---------|----------|
| **Monthly Recurring Revenue** | $15,000 | $40,000 |
| **Paying Customers** | 20 | 50 |
| **Customer Churn Rate** | <5%/mo | <3%/mo |
| **Customer Acquisition Cost** | <$1,500 | <$1,000 |
| **Lifetime Value** | >$12,000 | >$18,000 |

### 12.3 Technical Metrics

| Metric | Target |
|--------|--------|
| **API Uptime** | 99.9% |
| **Search Response (p95)** | <60s single, <1hr bulk |
| **WebSocket Latency** | <100ms |
| **Database Query (p95)** | <50ms |

---

## 13. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Privacy / Legal** | Medium | High | Heavy emphasis on legitimate interest, consent flows, template DPAs, annual legal review |
| **Data Accuracy** | Medium | Medium | Confidence scores with transparency; human review workflow; no 100% accuracy promises |
| **API Cost Overruns** | Medium | Medium | Credits model ensures margins; usage monitoring dashboard; monthly caps |
| **Adoption** | Medium | High | Free 50-credit trial; onboarding webinars; pilot program with hand-holding |
| **Competition** | Low | Medium | Differentiation via ethical positioning, heritage branding, education-specific features |

---

## 14. Appendix

### 14.1 Glossary
- **OSINT:** Open Source Intelligence - information from publicly available sources
- **PII:** Personally Identifiable Information
- **RLS:** Row-Level Security - database-level tenant isolation
- **NER:** Named Entity Recognition - NLP for identifying names, locations
- **MRR:** Monthly Recurring Revenue
- **LTV:** Lifetime Value
- **APPs:** Australian Privacy Principles

### 14.2 Related Documents
- Technical Design: `lost-alumni-finder-technical-design.md`
- API Specification: Auto-generated OpenAPI spec from FastAPI
- Security Policy: `security-bluebook.md` (to be created)
- Privacy Templates: Provided in-app for DPAs and consent notices

### 14.3 Change Log

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-30 | Initial PRD (Lost Alumni Finder) | August AI |
| 2.0 | 2026-03-30 | Merged AlumniBeacon branding, ethical framing, self-claim portal, updated pricing | August AI + Grok |

---

## Self-Critique & Confidence Assessment

### Strengths
- **Ethical/legal safeguards front-loaded:** Critical for Australia's Privacy Act 1988 and APPs
- **Clear differentiation:** AI-driven enrichment, multi-tenant isolation, education-specific compliance
- **Heritage branding:** 100% original, premium yet humble aesthetic
- **Balanced pricing:** Covers API costs while remaining accessible ($99-$399 tiers)
- **Production-ready stack:** Next.js 15, FastAPI, PostgreSQL with RLS, proven scalable

### Weaknesses Mitigated
- **"Aggressive scans" reframed:** Rate-limited, API-first, legally compliant—no ToS violations
- **High enrichment costs covered:** Tiered credits + overage protection
- **No 100% accuracy promised:** Confidence scoring + human review workflow
- **MVP achievable:** 12-week phased roadmap with clear deliverables

### Overall Confidence
**HIGH** — Feasible technical architecture, clear market fit, differentiated positioning, compliant by design. Ready for stakeholder review and engineering hand-off.

---

**END OF DOCUMENT**

*This PRD is a living document. All changes must be reviewed and approved by the product owner.*
