# August OSINT Gateway - Project Instructions

## Overview
This project is a Spring Boot web application that integrates with August/Agent Zero to provide OSINT (Open Source Intelligence) capabilities for locating lost alumni. The architecture is designed to be:
- **Cheap**: $0-7/month hosting cost
- **Multi-tenant**: Application-level isolation
- **Buildable**: Standard Spring Boot + SQLite stack

## Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    SINGLE CONTAINER                        │
│  ┌─────────────────────┐    ┌─────────────────────────┐  │
│  │  Spring Boot 3.4    │◄──►│  August OSINT Service   │  │
│  │  (Java 21)          │HTTP│  (Python/FastAPI)       │  │
│  │  Port: 8080         │    │  Port: localhost:8000    │  │
│  └─────────────────────┘    └─────────────────────────┘  │
│           │                            │                   │
│           ▼                            ▼                   │
│  ┌─────────────────────┐    ┌─────────────────────────┐  │
│  │  SQLite Database    │    │  OSINT APIs (External)  │  │
│  │  /data/osint.db     │    │  PDL, Apollo, etc.      │  │
│  └─────────────────────┘    └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Key Documents
- `docs/PRD-v2.0-merged.md` - Complete product requirements
- `docs/PRD-v1.0-original.md` - Original Lost Alumni Finder PRD
- `docs/ARCHITECTURE.md` - Technical architecture (to be created)
- `docs/API-SPEC.md` - API specification (to be created)

## Development Guidelines
1. Use Spring Boot 3.4 with Java 21
2. SQLite for database (file-based, zero cost)
3. Application-level multi-tenancy (tenant_id column)
4. Thymeleaf + HTMX for frontend (no React build step)
5. Both services in single Docker container
6. Target Render/Fly.io free tier

## Cost Constraints
- Hosting: $0-7/month (Render free tier)
- Database: $0 (SQLite)
- OSINT APIs: Pay-per-use, passed to customer
- Total operational: <$10/month at scale

## Activation
When this project is activated:
- Work directory: `/a0/usr/projects/august-osint-gateway/`
- All files saved relative to this directory
- Follow Spring Boot conventions
- Maintain multi-tenant security
