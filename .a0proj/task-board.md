# August OSINT Gateway - Task Board

## Option D — Agent Zero OSINT Profile

### Phase 1 — Profile Creation ✅ COMPLETE (2026-03-31)
- [x] Create `agents/alumnibeacon-osint/` directory structure
- [x] Write `agent.yaml` profile metadata (title, description, context)
- [x] Write `agent.system.main.role.md` (role, 7-step workflow, tool priority, budget rule, false positive prevention, compliance)
- [x] Write `agent.system.main.communication.md` (mandatory JSON output schema, confidence scoring, critical output rules)
- [x] Test profile manually — smoke test passed, valid JSON returned
- [x] Verify JSON output schema compliance — all 13 fields present and correct
- [x] Test context isolation — profile spawns fresh via call_subordinate with reset:true

### Phase 2 — Java Integration (Week 2)
- [ ] Update `AugustOsintAdapter.java` with dual-mode (A2A + Python fallback)
- [ ] Add `extractJsonFromA2AResponse()` parser
- [ ] Add new `application.properties` keys (`agent.zero.enabled`, `agent.zero.url`, etc.)
- [ ] Raise job queue stuck-job timeout to 20 minutes
- [ ] Add `agent.zero.enabled` feature flag
- [ ] Integration test: submit investigation → Agent Zero → parse result

### Phase 3 — UI Enhancements (Week 3)
- [ ] Add "Search Depth" toggle to investigation creation form
- [ ] Add `AGENT_THINKING` / `AGENT_SEARCHING` status states
- [ ] Update HTMX polling to show live status messages
- [ ] Add investigation engine badge to results display (🤖 Deep Investigation / ⚡ Standard Search)
- [ ] Add tool calls used / duration metadata to results

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

# August OSINT Gateway - Task Board

## Phase 1: Foundation (Week 1-2)
- [ ] Create Spring Boot 3.4 project skeleton
- [ ] Set up SQLite with JPA/Hibernate
- [ ] Implement JWT authentication
- [ ] Create tenant isolation layer
- [ ] Build August OSINT adapter (FastAPI)

## Phase 2: Core Features (Week 3-4)
- [ ] Investigation CRUD API
- [ ] SQLite-based job queue
- [ ] OSINT search integration
- [ ] Confidence scoring display
- [ ] PDF report generation

## Phase 3: Frontend (Week 5-6)
- [ ] Thymeleaf templates
- [ ] HTMX for reactivity
- [ ] Dashboard with investigation list
- [ ] Detail view with progress
- [ ] Create investigation wizard

## Phase 4: Integration & Deploy (Week 7-8)
- [ ] Docker containerisation
- [ ] Single-container orchestration
- [ ] Render deployment
- [ ] SSL/domain setup
- [ ] Monitoring and logging

## Backlog
- [ ] Alumni self-claim portal (v2)
- [ ] CRM integrations (Salesforce/HubSpot)
- [ ] Benchmark reports
- [ ] API access for enterprise
