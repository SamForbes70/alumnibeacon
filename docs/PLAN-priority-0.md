# AlumniBeacon — Priority 0 Implementation Plan
## Bug Fixes: Ship Day 1

**Date:** 31 March 2026  
**Scope:** 6 atomic tasks, ~4–6 hours total  
**Goal:** Zero Thymeleaf rendering errors, working HTMX polling, wired list stats, contactEmail saved

---

## Overview

| Task | File(s) | Effort | Risk |
|---|---|---|---|
| T-01 | `admin/index.html` | 15 min | Low |
| T-02 | `Tenant.java`, `AdminController.java`, `V2__migration.sql` | 30 min | Low |
| T-03 | `InvestigationController.java`, `InvestigationRepository.java`, `InvestigationService.java` | 45 min | Low |
| T-04 | `investigation/detail.html` | 30 min | Medium |
| T-05 | `SecurityConfig.java` | 20 min | Medium |
| T-06 | `investigation/list.html` | 10 min | Low |

**Verification:** After all tasks, run `mvn spring-boot:run` and manually test each page.

---

## T-01 — Fix Admin Template Variable Reference

**Bug:** `admin/index.html` references `${currentUser.userId}` but the controller passes `currentUserId`.

**Root cause:**  
`AdminController.adminIndex()` line:
```java
model.addAttribute("currentUserId", td.userId());  // passes "currentUserId"
```
Template uses:
```html
th:if="${user.id != currentUser.userId}"  // looks for "currentUser" object — WRONG
```

**Fix — `src/main/resources/templates/admin/index.html`:**

Find the team members loop (search for `currentUser.userId`):
```html
<!-- BEFORE -->
<form th:if="${user.id != currentUser.userId}"
```
```html
<!-- AFTER -->
<form th:if="${user.id != currentUserId}"
```
Also fix the "You" badge:
```html
<!-- BEFORE -->
<span th:if="${user.id == currentUser.userId}"
```
```html
<!-- AFTER -->
<span th:if="${user.id == currentUserId}"
```

**Verification:** Load `/admin` — no Thymeleaf `PropertyAccessException`. The "You" badge appears next to the logged-in user. Remove button absent for current user.

---

## T-02 — Add contactEmail to Tenant Model

**Bug:** `admin/index.html` renders `${tenant.contactEmail}` and the form POSTs `contactEmail`, but:
1. `Tenant.java` has no `contactEmail` field → Thymeleaf throws `PropertyAccessException`
2. `AdminController.updateSettings()` accepts `contactEmail` param but never saves it

### Step 1 — Add field to `Tenant.java`

**File:** `src/main/java/com/alumnibeacon/model/Tenant.java`

Add after the `slug` field (after line `private String slug;`):
```java
@Column(name = "contact_email")
private String contactEmail;
```

Full field block for context:
```java
@Column(nullable = false)
private String name;

@Column(nullable = false, unique = true)
private String slug;

// ADD THIS:
@Column(name = "contact_email")
private String contactEmail;

@Column(nullable = false)
@Enumerated(EnumType.STRING)
```

### Step 2 — Save contactEmail in AdminController

**File:** `src/main/java/com/alumnibeacon/controller/AdminController.java`

Find `updateSettings()` method. Change:
```java
// BEFORE
tenantRepository.findById(tenantId).ifPresent(tenant -> {
    tenant.setName(organisationName);
    tenantRepository.save(tenant);
});
```
```java
// AFTER
tenantRepository.findById(tenantId).ifPresent(tenant -> {
    tenant.setName(organisationName);
    if (contactEmail != null && !contactEmail.isBlank()) {
        tenant.setContactEmail(contactEmail);
    }
    tenantRepository.save(tenant);
});
```

### Step 3 — Add Flyway migration

**New file:** `src/main/resources/db/migration/V2__add_contact_email.sql`

```sql
-- V2: Add contact_email to tenants table
ALTER TABLE tenants ADD COLUMN contact_email TEXT;
```

**Verification:**
1. App starts without Flyway errors
2. Load `/admin` — no Thymeleaf error on `${tenant.contactEmail}`
3. Update org name + contact email → save → values persist on reload

---

## T-03 — Wire Missing Model Attributes in Investigation List

**Bug:** `investigation/list.html` references 7 model attributes that `InvestigationController.list()` never adds:
- `${totalCount}` — total investigations
- `${completedCount}` — completed count
- `${processingCount}` — in-progress count  
- `${creditsUsed}` — credits consumed
- `${totalPages}` — for pagination
- `${currentPage}` — for pagination
- `${pageSize}` — for pagination

Also: the search input and status filter use HTMX `hx-get="/investigations"` with `name="search"` and `name="status"` params, but the controller ignores them.

### Step 1 — Add `countByStatus` to InvestigationRepository

**File:** `src/main/java/com/alumnibeacon/repository/InvestigationRepository.java`

Add after `countCompletedByTenantId`:
```java
@Query("SELECT COUNT(i) FROM Investigation i WHERE i.tenantId = :tenantId AND i.status = :status")
long countByTenantIdAndStatus(@Param("tenantId") String tenantId, @Param("status") String status);

@Query("SELECT i FROM Investigation i WHERE i.tenantId = :tenantId "
     + "AND (:search = '' OR LOWER(i.subjectName) LIKE LOWER(CONCAT('%', :search, '%'))) "
     + "AND (:status = '' OR i.status = :status) "
     + "ORDER BY i.createdAt DESC")
List<Investigation> findByTenantFiltered(
    @Param("tenantId") String tenantId,
    @Param("search") String search,
    @Param("status") String status
);
```

### Step 2 — Add `listByTenantFiltered` to InvestigationService

**File:** `src/main/java/com/alumnibeacon/service/InvestigationService.java`

Add after `listByTenant()`:
```java
public List<InvestigationDto> listByTenantFiltered(String tenantId, String search, String status) {
    String safeSearch = search != null ? search.trim() : "";
    String safeStatus = status != null ? status.trim() : "";
    return investigationRepository.findByTenantFiltered(tenantId, safeSearch, safeStatus)
        .stream().map(InvestigationDto::from).toList();
}
```

### Step 3 — Update InvestigationController.list()

**File:** `src/main/java/com/alumnibeacon/controller/InvestigationController.java`

Replace the entire `list()` method:
```java
// BEFORE
@GetMapping("/investigations")
public String list(Authentication auth, Model model) {
    TenantDetails td = (TenantDetails) auth.getDetails();
    List<InvestigationDto> investigations = investigationService.listByTenant(td.tenantId());
    model.addAttribute("investigations", investigations);
    model.addAttribute("creditsRemaining", creditService.getBalance(td.tenantId()));
    return "investigation/list";
}
```

```java
// AFTER
@GetMapping("/investigations")
public String list(
        Authentication auth,
        Model model,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "") String status,
        @RequestParam(defaultValue = "0") int page) {

    TenantDetails td = (TenantDetails) auth.getDetails();
    String tenantId = td.tenantId();

    List<InvestigationDto> investigations =
            investigationService.listByTenantFiltered(tenantId, search, status);

    // Stats for the stats bar
    long totalCount      = investigationService.countByTenant(tenantId);
    long completedCount  = investigationService.countByTenantAndStatus(tenantId, "COMPLETED");
    long processingCount = investigationService.countByTenantAndStatus(tenantId, "PROCESSING");
    int  creditsUsed     = creditService.getUsed(tenantId);

    // Pagination (simple slice — no Spring Data Page needed yet)
    int pageSize   = 20;
    int totalPages = (int) Math.ceil((double) investigations.size() / pageSize);
    int safePage   = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
    int fromIdx    = safePage * pageSize;
    int toIdx      = Math.min(fromIdx + pageSize, investigations.size());
    List<InvestigationDto> pageItems = investigations.subList(fromIdx, toIdx);

    model.addAttribute("investigations",  pageItems);
    model.addAttribute("totalCount",      totalCount);
    model.addAttribute("completedCount",  completedCount);
    model.addAttribute("processingCount", processingCount);
    model.addAttribute("creditsUsed",     creditsUsed);
    model.addAttribute("creditsRemaining",creditService.getBalance(tenantId));
    model.addAttribute("totalPages",      totalPages);
    model.addAttribute("currentPage",     safePage);
    model.addAttribute("pageSize",        pageSize);
    model.addAttribute("search",          search);
    model.addAttribute("status",          status);

    return "investigation/list";
}
```

### Step 4 — Add helper methods to InvestigationService

**File:** `src/main/java/com/alumnibeacon/service/InvestigationService.java`

Add after `listByTenantFiltered()`:
```java
public long countByTenant(String tenantId) {
    return investigationRepository.countByTenantId(tenantId);
}

public long countByTenantAndStatus(String tenantId, String status) {
    return investigationRepository.countByTenantIdAndStatus(tenantId, status);
}
```

**Verification:**
1. Load `/investigations` — stats bar shows real numbers (not 0/null)
2. Type in search box — table filters by name (HTMX partial update)
3. Change status dropdown — table filters by status
4. If >20 investigations exist, pagination Previous/Next links appear

---

## T-04 — Fix HTMX Polling in Investigation Detail

**Bug:** The HTMX polling block in `detail.html` has two problems:

**Problem A:** `hx-on::after-request` tries to parse the response as JSON:
```javascript
JSON.parse(event.detail.xhr.responseText).status === 'COMPLETED'
```
But the status endpoint (`/investigations/{id}/status`) returns an **HTML fragment** (`investigation/detail :: statusCard`), not JSON. This throws a JSON parse error every 5 seconds.

**Problem B:** The `statusCard` fragment is referenced by the endpoint but **does not exist** in `detail.html`. The endpoint would throw a Thymeleaf `FragmentNotFoundException`.

**Fix strategy:** Replace the broken HTMX approach with a clean polling pattern:
- Add a `statusCard` Thymeleaf fragment to `detail.html`
- Change the polling div to use `hx-swap="outerHTML"` to replace itself with the fragment
- On COMPLETED/FAILED, the fragment includes a `<meta http-equiv="refresh">` to trigger a full page reload
- This is simpler, more reliable, and requires no JavaScript JSON parsing

### Step 1 — Add statusCard fragment to detail.html

**File:** `src/main/resources/templates/investigation/detail.html`

Find the status card grid div (the `grid grid-cols-3 gap-6 mb-8` div). Wrap it in a fragment:

```html
<!-- BEFORE -->
<div class="grid grid-cols-3 gap-6 mb-8">
```

```html
<!-- AFTER -->
<div th:fragment="statusCard" class="grid grid-cols-3 gap-6 mb-8">
```

### Step 2 — Fix the HTMX polling div

Find the polling div (search for `hx-get=` in detail.html). Replace the entire polling block:

```html
<!-- BEFORE -->
<div th:if="${investigation.status == 'PENDING' or investigation.status == 'PROCESSING'}">
    <div class="bg-blue-50 border border-blue-200 rounded-xl p-6 mb-6"
         hx-get="/investigations/__${investigation.id}__/status"
         hx-trigger="every 5s"
         hx-swap="none"
         hx-on::after-request="if(JSON.parse(event.detail.xhr.responseText).status==='COMPLETED'||JSON.parse(event.detail.xhr.responseText).status==='FAILED'){window.location.reload()}">
        <div class="flex items-center gap-3">
            <div class="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            <p class="text-blue-700 font-medium">August OSINT search in progress...</p>
        </div>
        <p class="text-blue-500 text-sm mt-2">This page will automatically update when results are ready. Typical search time: 30-90 seconds.</p>
    </div>
</div>
```

```html
<!-- AFTER -->
<div th:if="${investigation.status == 'PENDING' or investigation.status == 'PROCESSING'}"
     id="polling-banner"
     th:attr="hx-get=@{/investigations/{id}/status(id=${investigation.id})}"
     hx-trigger="every 5s"
     hx-target="#polling-banner"
     hx-swap="outerHTML">
    <div class="bg-blue-50 border border-blue-200 rounded-xl p-6 mb-6">
        <div class="flex items-center gap-3">
            <div class="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            <p class="text-blue-700 font-medium">August OSINT search in progress...</p>
        </div>
        <p class="text-blue-500 text-sm mt-2">This page will automatically update when results are ready. Typical search time: 30–90 seconds.</p>
    </div>
</div>
```

### Step 3 — Update the status endpoint to return a reload trigger

**File:** `src/main/java/com/alumnibeacon/controller/InvestigationController.java`

Replace the `statusFragment()` method:
```java
// BEFORE
@GetMapping("/investigations/{id}/status")
public String statusFragment(@PathVariable String id,
                              Authentication auth,
                              Model model) {
    TenantDetails td = (TenantDetails) auth.getDetails();
    InvestigationDto inv = investigationService.getById(id, td.tenantId());
    model.addAttribute("investigation", inv);
    return "investigation/detail :: statusCard";
}
```

```java
// AFTER
@GetMapping("/investigations/{id}/status")
public String statusFragment(@PathVariable String id,
                              Authentication auth,
                              Model model,
                              jakarta.servlet.http.HttpServletResponse response) {
    TenantDetails td = (TenantDetails) auth.getDetails();
    InvestigationDto inv = investigationService.getById(id, td.tenantId());
    model.addAttribute("investigation", inv);

    // When complete or failed, tell HTMX to do a full page refresh
    if ("COMPLETED".equals(inv.status()) || "FAILED".equals(inv.status())) {
        response.setHeader("HX-Refresh", "true");
    }

    return "investigation/detail :: statusCard";
}
```

> **Why `HX-Refresh: true`?**  
> When HTMX receives this response header it triggers a full page reload. This is the cleanest way to transition from the polling state to the full results view without complex JavaScript.

**Verification:**
1. Create a new investigation
2. Navigate to detail page — spinning banner appears
3. Every 5 seconds the banner polls the status endpoint
4. When the job completes, the page automatically reloads and shows results
5. No JavaScript console errors

---

## T-05 — Fix SecurityConfig Session Policy Contradiction

**Bug:** `SecurityConfig.java` declares `STATELESS` sessions but also configures `formLogin`. These are contradictory:
- `STATELESS` means Spring Security never creates an `HttpSession`
- `formLogin` requires a session to store the authentication between the login POST and the redirect
- In practice, the JWT filter handles auth so `formLogin` is dead code — but it may cause unexpected behaviour on some Spring Boot versions

**Secondary issue:** The `formLogin` config points to `/login` as the login page, which is correct, but the `loginProcessingUrl` is not set, defaulting to `/login` POST — which conflicts with `AuthController`'s `POST /auth/login`.

**Fix — `src/main/java/com/alumnibeacon/config/SecurityConfig.java`:**

Remove the `formLogin` block entirely. The JWT filter handles all authentication.

```java
// BEFORE
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**",
                "/login",
                "/register",
                "/css/**",
                "/js/**",
                "/images/**",
                "/webjars/**",
                "/actuator/health",
                "/error"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .deleteCookies("jwt")
            .permitAll()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

```java
// AFTER
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // CSRF disabled — JWT cookie auth, not form-based sessions
        .csrf(AbstractHttpConfigurer::disable)
        // Stateless — no HttpSession created by Spring Security
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**",
                "/login",
                "/register",
                "/css/**",
                "/js/**",
                "/images/**",
                "/webjars/**",
                "/actuator/health",
                "/error"
            ).permitAll()
            .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
            .anyRequest().authenticated()
        )
        // No formLogin — JWT filter handles authentication
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .deleteCookies("jwt")
            .permitAll()
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

> **Note:** Added `.requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")` to enforce role-based access on the admin panel. Verify that `TenantDetails` grants the correct Spring Security role (check `JwtAuthFilter` — it should call `new SimpleGrantedAuthority("ROLE_" + user.getRole())`).

**Verification:**
1. App starts without errors
2. Unauthenticated user hitting `/investigations` is redirected to `/login`
3. Non-admin user hitting `/admin` gets 403
4. Admin user can access `/admin`
5. Logout clears the `jwt` cookie and redirects to `/login?logout`

---

## T-06 — Fix Retry Button Fetch (Missing Credentials)

**Bug:** The retry button in `investigation/list.html` uses `fetch()` without `credentials: 'include'`. Since auth is cookie-based (JWT in httpOnly cookie), the fetch won't send the cookie and will get a 401/redirect.

**File:** `src/main/resources/templates/investigation/list.html`

Find the `rerunInvestigation` script at the bottom:

```javascript
// BEFORE
function rerunInvestigation(id) {
    if (confirm('Retry this investigation? This will use 1 credit.')) {
        fetch('/investigations/' + id + '/retry', {method: 'POST',
            headers: {'X-CSRF-TOKEN': document.querySelector('meta[name=_csrf]')?.content || ''}})
            .then(() => window.location.reload());
    }
}
```

```javascript
// AFTER
function rerunInvestigation(id) {
    if (confirm('Retry this investigation? This will use 1 credit.')) {
        fetch('/investigations/' + id + '/retry', {
            method: 'POST',
            credentials: 'include'  // send JWT cookie
        })
        .then(response => {
            if (response.ok || response.redirected) {
                window.location.reload();
            } else {
                alert('Retry failed. Please try again.');
            }
        })
        .catch(() => alert('Network error. Please try again.'));
    }
}
```

**Verification:**
1. Create an investigation, let it fail
2. On the list page, click Retry
3. Confirm dialog appears
4. Investigation status changes to PENDING
5. Page reloads showing updated status

---

## Execution Order

Execute tasks in this order to minimise risk:

```
1. T-01  admin template variable fix          (no compilation needed)
2. T-02  contactEmail field + migration       (requires app restart)
3. T-05  SecurityConfig cleanup               (requires app restart)
4. T-03  list controller + repository         (requires app restart)
5. T-04  HTMX polling fix                     (requires app restart)
6. T-06  retry fetch fix                      (no compilation needed)
```

---

## Build & Test Commands

```bash
# From project root
cd /a0/usr/projects/aulumnibeacon

# Compile only (fast check)
mvn compile -q

# Run tests
mvn test

# Start app (dev mode)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Verify app started
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

---

## Manual Test Checklist

After all 6 tasks are complete, verify each page:

### Auth
- [ ] `GET /login` — login page loads
- [ ] `POST /auth/login` with valid credentials — redirects to `/dashboard`, `jwt` cookie set
- [ ] `GET /logout` — cookie cleared, redirected to `/login?logout`

### Dashboard
- [ ] `GET /dashboard` — loads without errors, shows stats

### Investigations List
- [ ] `GET /investigations` — loads, stats bar shows real numbers
- [ ] Type in search box — table filters (HTMX partial update, no full reload)
- [ ] Change status dropdown — table filters
- [ ] Stats bar: TOTAL, COMPLETED, IN PROGRESS, CREDITS USED all show correct values

### Investigation Detail
- [ ] `GET /investigations/{id}` for PENDING/PROCESSING — spinning banner visible
- [ ] Wait 5 seconds — no JavaScript console errors
- [ ] When job completes — page auto-reloads showing results
- [ ] `GET /investigations/{id}` for COMPLETED — results section visible (raw JSON for now)
- [ ] `GET /investigations/{id}/report` for COMPLETED — PDF downloads

### Admin Panel
- [ ] `GET /admin` — loads without Thymeleaf errors
- [ ] "You" badge appears next to current user
- [ ] Remove button absent for current user
- [ ] Update org name + contact email → save → values persist on reload
- [ ] Non-admin user hitting `/admin` → 403

### Retry
- [ ] Failed investigation on list page → click Retry → confirm → status changes to PENDING

---

## Definition of Done

Priority 0 is complete when:
- [ ] `mvn compile` passes with zero errors
- [ ] `mvn test` passes (or no tests exist yet — acceptable)
- [ ] All 12 manual test checklist items pass
- [ ] No Thymeleaf `PropertyAccessException` or `FragmentNotFoundException` in logs
- [ ] No JavaScript console errors on any page
- [ ] `git commit -m "fix: priority 0 bug fixes — admin template, contactEmail, list stats, HTMX polling, security config, retry fetch"`
