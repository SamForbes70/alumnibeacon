# Lost Alumni Finder - Product Requirements Document (PRD)

**Version:** 1.0  
**Date:** March 30, 2026  
**Status:** Draft for Review  
**Author:** August AI Assistant  

---

## 1. Executive Summary

### 1.1 Product Vision
Lost Alumni Finder is a **B2B SaaS platform** that empowers educational institutions (schools, universities, alumni associations) to locate and reconnect with lost alumni using advanced OSINT (Open Source Intelligence) techniques. The platform automates the investigation process through a 5-stage workflow, delivering confidence-scored results with professional evidence packaging.

### 1.2 Target Market
- **Primary:** Private schools, universities, and alumni associations
- **Secondary:** Professional associations, non-profits, corporate alumni networks
- **Geographic:** Initially Australia/NZ, expandable to global markets

### 1.3 Value Proposition
- **Automated Investigation:** Reduces manual research time by 80%
- **Confidence Scoring:** Traffic-light system (HIGH/MEDIUM/LOW) for result reliability
- **Compliance-First:** GDPR/privacy-compliant data handling with automatic PII redaction
- **Professional Reports:** Court-ready evidence packages with chain of custody

### 1.4 Business Model
- **Pricing:** Subscription-based ($500 - $2,000/month)
- **Revenue Target:** $15k - $40k MRR within 12 months
- **Sales Cycle:** 30-60 days (B2B institutional sales)

---

## 2. Product Overview

### 2.1 Core Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Investigation Management** | Create, track, and manage alumni search cases | P0 |
| **5-Stage Workflow** | Automated pipeline: Initiation → Investigation → Analysis → QC → Delivery | P0 |
| **Real-Time Progress** | Live progress tracking via WebSocket updates | P0 |
| **Confidence Scoring** | Multi-factor scoring algorithm with quality metrics | P0 |
| **Evidence Packaging** | Professional PDF reports with source citations | P0 |
| **Batch Processing** | Bulk alumni imports and parallel investigations | P1 |
| **Scheduled Reports** | Automated recurring searches and stale data alerts | P1 |
| **API Access** | RESTful API for institutional integrations | P2 |

### 2.2 User Roles

| Role | Permissions | Use Case |
|------|-------------|----------|
| **Administrator** | Full system access, user management, billing | IT/Operations |
| **Investigator** | Create investigations, view results, download reports | Alumni Relations Staff |
| **Viewer** | Read-only access to investigations and reports | Management |
| **API User** | Programmatic access via API keys | System Integrations |

---

## 3. Functional Requirements

### 3.1 Investigation Workflow (5-Stage)

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
  - Validate required fields (name, DOB/graduation year)
  - Generate unique investigation ID (UUID)
  - Queue job in Redis
  - Create initial database record (status: PENDING)
- **Output:** Investigation queued confirmation

#### Stage 2: Investigation
- **Trigger:** Job picked up by worker
- **Actions:**
  - Corporate intelligence (LinkedIn, company directories)
  - Social media enumeration (Facebook, Instagram, Twitter/X)
  - Public records search (electoral rolls, property records)
  - Professional association lookups
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
  - Confidence scoring algorithm
  - Cross-reference verification
  - Data freshness assessment
- **Output:** Confidence score, status: QC

#### Stage 5: Delivery
- **Trigger:** QC passed
- **Actions:**
  - Generate professional PDF report
  - Package evidence with chain of custody
  - Update investigation record (status: COMPLETED)
  - Notify user
- **Output:** Downloadable report, status: COMPLETED

### 3.2 Confidence Scoring Algorithm

**Multi-Factor Scoring (0-100%):**

| Factor | Weight | Description |
|--------|--------|-------------|
| **Data Points** | 30% | Number of corroborating data sources |
| **Cross-References** | 25% | Consistency across multiple platforms |
| **Verification Level** | 25% | Strength of identity verification |
| **Recency** | 10% | Freshness of data (0-12 months optimal) |
| **Source Quality** | 10% | Reliability of data sources |

**Traffic Light System:**
- **HIGH (≥85%):** Strong confidence, actionable intelligence
- **MEDIUM (60-84%):** Moderate confidence, may require manual verification
- **LOW (<60%):** Weak confidence, likely false positive

### 3.3 Data Redaction & Privacy

**Automatic PII Handling:**
- SHA-256 hashing for sensitive identifiers
- Spacy-based named entity recognition for redaction
- GDPR-compliant data retention (auto-delete after 90 days)
- Audit logging for all data access

---

## 4. Technical Architecture

### 4.1 System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      CLIENT LAYER                                        │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────────────────┐  │
│  │   Next.js 14 Frontend           │  │   API Consumers (Integrations)              │  │
│  │   • React 18 + TypeScript       │  │   • Webhooks                                │  │
│  │   • Tailwind CSS + Radix UI     │  │   • Third-party systems                     │  │
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
│  │  • Asyncpg      │    │  │  │  • Job Queue    │    │  │  │  • Reports      │    │
│  │  • Connection   │    │  │  │  • Pub/Sub      │    │  │  │  • Evidence     │    │
│  │    Pooling      │    │  │  │  • Session      │    │  │  │  • Backups      │    │
│  │  • JSONB for    │    │  │  │    Cache        │    │  │  │                 │    │
│  │    findings     │    │  │  └─────────────────┘    │  │  └─────────────────┘    │
│  └─────────────────┘    │  └─────────────────────────┘  └─────────────────────────┘
└─────────────────────────┘
```

### 4.2 Technology Stack

| Layer | Technology | Version | Purpose |
|-------|------------|---------|---------|
| **Frontend** | Next.js | 14.2+ | React framework with App Router |
| | TypeScript | 5.0+ | Type safety |
| | Tailwind CSS | 3.4+ | Utility-first styling |
| | Radix UI | Latest | Accessible UI primitives |
| | Lucide React | 0.576+ | Icon library |
| **Backend** | FastAPI | 0.100+ | Async Python web framework |
| | Pydantic | 2.0+ | Data validation |
| | Uvicorn | 0.23+ | ASGI server |
| | Python | 3.12+ | Runtime |
| **Database** | PostgreSQL | 15+ | Primary data store |
| | Asyncpg | 0.28+ | Async PostgreSQL driver |
| | Redis | 7+ | Job queue & caching |
| **Storage** | AWS S3 | - | Report & evidence storage |
| **Auth** | python-jose | 3.3+ | JWT tokens |
| | passlib | 1.7+ | Password hashing |

### 4.3 Database Schema

```sql
-- Investigations table
CREATE TABLE investigations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_name VARCHAR(255) NOT NULL,
    subject_dob DATE,
    subject_last_known_location VARCHAR(255),
    subject_school_years VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, QUEUED, RUNNING, COMPLETED, FAILED
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    confidence_score DECIMAL(5,2),
    progress_percent INTEGER DEFAULT 0,
    current_stage VARCHAR(50),
    findings JSONB,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_by UUID REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id)
);

-- Job logs table
CREATE TABLE job_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investigation_id UUID NOT NULL REFERENCES investigations(id) ON DELETE CASCADE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    level VARCHAR(20) NOT NULL, -- INFO, WARN, ERROR
    message TEXT NOT NULL,
    stage VARCHAR(50),
    metadata JSONB
);

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'investigator', -- admin, investigator, viewer
    organization_id UUID REFERENCES organizations(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE
);

-- Organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    subscription_tier VARCHAR(50) DEFAULT 'starter', -- starter, professional, enterprise
    subscription_status VARCHAR(50) DEFAULT 'active',
    max_investigations_per_month INTEGER DEFAULT 100,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_investigations_status ON investigations(status);
CREATE INDEX idx_investigations_created_at ON investigations(created_at);
CREATE INDEX idx_investigations_org ON investigations(organization_id);
CREATE INDEX idx_job_logs_investigation ON job_logs(investigation_id);
CREATE INDEX idx_job_logs_timestamp ON job_logs(timestamp);
```

---

## 5. API Specifications

### 5.1 REST API Endpoints

#### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | JWT token acquisition |
| POST | `/api/v1/auth/refresh` | Token refresh |
| POST | `/api/v1/auth/logout` | Token revocation |

#### Investigations
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/investigations` | List all investigations (paginated) |
| POST | `/api/v1/investigations` | Create new investigation |
| GET | `/api/v1/investigations/{id}` | Get investigation details |
| PATCH | `/api/v1/investigations/{id}` | Update investigation |
| DELETE | `/api/v1/investigations/{id}` | Delete investigation |
| POST | `/api/v1/investigations/{id}/rerun` | Re-run investigation |

#### Jobs & Progress
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/investigations/{id}/jobs` | Get job status and logs |
| GET | `/api/v1/investigations/{id}/reports/download` | Download PDF report |

#### WebSocket
| Endpoint | Description |
|----------|-------------|
| `/ws/investigations/{id}` | Real-time progress updates |

### 5.2 Pydantic Models

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
    data_points: int
    cross_references: int
    verification_level: str  # HIGH, MEDIUM, LOW
    recency_score: int
    source_quality: str
    overall_score: float

class InvestigationResponse(BaseModel):
    id: str
    subject_name: str
    subject_dob: Optional[str]
    subject_last_known_location: Optional[str]
    subject_school_years: Optional[str]
    status: InvestigationStatus
    priority: Priority
    confidence_score: Optional[float]
    progress_percent: int
    current_stage: Optional[str]
    findings: Optional[InvestigationFindings]
    confidence_breakdown: Optional[ConfidenceBreakdown]
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

## 6. UI/UX Design

### 6.1 Design System

**Color Palette (Dark/Glassmorphism Theme):**

| Token | Hex | Usage |
|-------|-----|-------|
| `--slate-950` | #020617 | Background |
| `--slate-900` | #0f172a | Card backgrounds |
| `--slate-800` | #1e293b | Borders, dividers |
| `--slate-400` | #94a3b8 | Secondary text |
| `--slate-200` | #e2e8f0 | Primary text |
| `--emerald-500` | #10b981 | Success, HIGH confidence |
| `--emerald-400` | #34d399 | Progress bars |
| `--amber-500` | #f59e0b | Warning, MEDIUM confidence |
| `--red-500` | #ef4444 | Error, LOW confidence |
| `--blue-500` | #3b82f6 | Primary actions |
| `--violet-500` | #8b5cf6 | Accent |

**Typography:**
- **Font Family:** Inter (sans-serif)
- **Headings:** 600-700 weight
- **Body:** 400 weight
- **Monospace:** JetBrains Mono (for logs, timestamps)

### 6.2 Key UI Components

#### InvestigationRow (Dashboard List Item)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ [Avatar]  Dale Bigham                    [COMPLETED] [HIGH]              [Actions ▼] │
│           Class of 1986 • Melbourne                  92% confidence                  │
│                                                      Completed 2 hours ago           │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**States:**
- **PENDING:** Clock icon, gray badge, 0% progress
- **RUNNING:** Animated spinner, blue badge, live progress bar
- **COMPLETED:** CheckCircle icon, green badge, confidence score
- **FAILED:** AlertCircle icon, red badge, error message

#### Investigation Detail Page
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ Dale Bigham Investigation                                    [Re-run] [Download ▼] │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────────────┐   │
│  │ STATUS CARD                     │  │ FINDINGS                                │   │
│  │ ┌─────────────────────────────┐ │  │ • Locations: Melbourne, VIC             │   │
│  │ │ [✓] COMPLETED               │ │  │ • Employment: BHP (2019-present)        │   │
│  │ │                             │ │  │ • LinkedIn: linkedin.com/in/...         │   │
│  │ │ Confidence: 92%             │ │  │ • Contact: dale.bigham@email.com        │   │
│  │ │ Duration: 20 minutes        │ │  │                                         │   │
│  │ │                             │ │  │ Confidence Breakdown:                   │   │
│  │ │ [████████░░] 92%            │ │  │ • Data Points: 12/15 (30%)              │   │
│  │ └─────────────────────────────┘ │  │ • Cross-References: 8/10 (25%)          │   │
│  │                                 │  │ • Verification: HIGH (25%)              │   │
│  │ [Overview] [Logs] [Evidence]    │  │ • Recency: 10/10 (10%)                  │   │
│  │                                 │  │ • Source Quality: A (10%)               │   │
│  └─────────────────────────────────┘  └─────────────────────────────────────────┘   │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

#### Create Investigation Modal
```
┌─────────────────────────────────────────────────────────┐
│ Create New Investigation                           [×]  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Subject Name *                                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Dale Bigham                                      │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Date of Birth                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 1970-03-15                                       │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Last Known Location                                    │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Melbourne, Victoria                              │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  School Years                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 1982-1986                                        │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Priority                    [Medium ▼]                 │
│                                                         │
│  Notes                                                  │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Additional context...                            │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│                              [Cancel]  [Start Investigation] │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 6.3 Responsive Breakpoints

| Breakpoint | Width | Layout Adjustments |
|------------|-------|-------------------|
| Mobile | < 640px | Single column, stacked cards, hamburger menu |
| Tablet | 640-1024px | Two-column grid, condensed sidebar |
| Desktop | > 1024px | Full three-column layout, expanded sidebar |

---

## 7. Security & Compliance

### 7.1 Authentication & Authorization
- **JWT Tokens:** Access tokens (15 min expiry), refresh tokens (7 days)
- **Password Policy:** Min 12 chars, uppercase, lowercase, number, special char
- **Role-Based Access Control (RBAC):** Admin, Investigator, Viewer roles
- **API Keys:** For programmatic access with scoped permissions

### 7.2 Data Protection
- **Encryption at Rest:** AES-256 for database and S3 storage
- **Encryption in Transit:** TLS 1.3 for all connections
- **PII Handling:** Automatic redaction using NER (Named Entity Recognition)
- **Data Retention:** Auto-delete investigation data after 90 days
- **Audit Logging:** All data access logged with user ID and timestamp

### 7.3 Compliance
- **GDPR:** Right to erasure, data portability, consent management
- **Privacy by Design:** Data minimization, purpose limitation
- **SOC 2 Type II:** Target certification within 12 months of launch

---

## 8. Business Model & Pricing

### 8.1 Subscription Tiers

| Feature | Starter | Professional | Enterprise |
|---------|---------|--------------|------------|
| **Price** | $500/mo | $1,000/mo | $2,000/mo |
| **Investigations/Month** | 50 | 200 | Unlimited |
| **Users** | 3 | 10 | Unlimited |
| **Batch Processing** | ❌ | ✅ | ✅ |
| **API Access** | ❌ | ✅ | ✅ |
| **Priority Support** | Email | Email + Chat | Dedicated |
| **Custom Integrations** | ❌ | ❌ | ✅ |
| **SLA** | 99.5% | 99.9% | 99.99% |

### 8.2 Revenue Projections

| Month | Target MRR | Cumulative |
|-------|-----------|------------|
| 1-3 | $5,000 | $15,000 |
| 4-6 | $15,000 | $60,000 |
| 7-9 | $25,000 | $135,000 |
| 10-12 | $40,000 | $255,000 |

### 8.3 Customer Acquisition Strategy
- **Outbound:** Direct sales to alumni associations (50 schools target)
- **Inbound:** Content marketing (OSINT guides, case studies)
- **Partnerships:** Integration with alumni management platforms
- **Referrals:** 10% discount for customer referrals

---

## 9. Implementation Roadmap

### 9.1 Phase 1: MVP (Weeks 1-4)
**Goal:** Core functionality for single-user testing

| Week | Deliverables |
|------|-------------|
| 1 | Database schema, FastAPI skeleton, authentication |
| 2 | Investigation CRUD API, job queue (Redis), basic frontend |
| 3 | 5-stage workflow implementation, WebSocket progress |
| 4 | PDF report generation, confidence scoring, testing |

### 9.2 Phase 2: Beta (Weeks 5-8)
**Goal:** Multi-tenant support, 3 pilot customers

| Week | Deliverables |
|------|-------------|
| 5 | Organization multi-tenancy, user management |
| 6 | Batch processing, scheduled reports |
| 7 | Advanced OSINT integrations, false positive detection |
| 8 | Pilot customer onboarding, feedback iteration |

### 9.3 Phase 3: Launch (Weeks 9-12)
**Goal:** Production-ready, public launch

| Week | Deliverables |
|------|-------------|
| 9 | API documentation, developer portal |
| 10 | Security audit, SOC 2 preparation |
| 11 | Marketing website, documentation |
| 12 | Public launch, PR campaign |

---

## 10. Success Metrics

### 10.1 Product Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Investigation Success Rate** | >85% | % completed with HIGH confidence |
| **Average Investigation Time** | <30 min | Time from start to completion |
| **False Positive Rate** | <10% | % LOW confidence results |
| **User Satisfaction (NPS)** | >50 | Quarterly surveys |

### 10.2 Business Metrics

| Metric | 6-Month Target | 12-Month Target |
|--------|---------------|-----------------|
| **Monthly Recurring Revenue** | $15,000 | $40,000 |
| **Paying Customers** | 15 | 40 |
| **Customer Churn Rate** | <5%/month | <3%/month |
| **Customer Acquisition Cost** | <$2,000 | <$1,500 |
| **Lifetime Value** | >$12,000 | >$18,000 |

### 10.3 Technical Metrics

| Metric | Target |
|--------|--------|
| **API Uptime** | 99.9% |
| **API Response Time (p95)** | <200ms |
| **WebSocket Latency** | <100ms |
| **Database Query Time (p95)** | <50ms |

---

## 11. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Data Source Changes** | High | High | Abstract data sources, multiple providers |
| **Privacy Regulations** | Medium | High | Privacy-by-design, legal review |
| **Competition** | Medium | Medium | Differentiation via confidence scoring |
| **Technical Debt** | Medium | Medium | Code reviews, testing, documentation |
| **Customer Acquisition** | Medium | High | Diversified channels, pilot programs |

---

## 12. Appendix

### 12.1 Glossary
- **OSINT:** Open Source Intelligence - information collected from publicly available sources
- **PII:** Personally Identifiable Information
- **NER:** Named Entity Recognition - NLP technique for identifying names, locations, etc.
- **MRR:** Monthly Recurring Revenue
- **LTV:** Lifetime Value

### 12.2 Related Documents
- Technical Design Document: `lost-alumni-finder-technical-design.md`
- API Specification: OpenAPI spec (auto-generated from FastAPI)
- Security Policy: `security-bluebook.md` (to be created)

### 12.3 Change Log

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-03-30 | Initial PRD | August AI |

---

**END OF DOCUMENT**

*This PRD is a living document. All changes must be reviewed and approved by the product owner.*
