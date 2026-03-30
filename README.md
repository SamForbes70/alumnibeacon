# August OSINT Gateway

**A Spring Boot web application that wraps August/Agent Zero OSINT capabilities.**

## Quick Links
- [Product Requirements (v2.0)](docs/PRD-v2.0-merged.md)
- [Original PRD (v1.0)](docs/PRD-v1.0-original.md)
- [Project Config](.a0proj/project-config.json)

## Architecture
Single-container deployment with:
- **Spring Boot 3.4** (Java 21) - Web layer, security, multi-tenancy
- **SQLite 3** - Zero-cost database with WAL mode
- **August OSINT Service** (Python/FastAPI) - Intelligence layer wrapper
- **Thymeleaf + HTMX** - Server-rendered reactive UI

## Cost Target
**$0-7/month** operational cost using Render free tier + SQLite.

## Status
📋 Planning phase - PRD complete, ready for implementation

## Next Steps
1. Create Spring Boot skeleton
2. Build August OSINT adapter (FastAPI wrapper)
3. Implement SQLite schema with multi-tenancy
4. Create Thymeleaf + HTMX frontend
5. Docker containerisation
6. Deploy to Render free tier
