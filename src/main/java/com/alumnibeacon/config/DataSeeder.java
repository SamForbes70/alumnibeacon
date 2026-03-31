package com.alumnibeacon.config;

import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.model.Tenant;
import com.alumnibeacon.model.User;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.TenantRepository;
import com.alumnibeacon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Seeds the database with test data for development and testing.
 * Only active when spring.profiles.active=dev.
 * Idempotent — checks before inserting.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TenantRepository       tenantRepository;
    private final UserRepository         userRepository;
    private final InvestigationRepository investigationRepository;
    private final PasswordEncoder        passwordEncoder;

    static final String TENANT_ID   = "tenant-test-001";
    static final String USER_ID     = "user-test-001";
    static final String ADMIN_EMAIL = "admin@utas.edu.au";
    static final String ADMIN_PASS  = "password123";

    @Override
    public void run(String... args) {
        seedTenant();
        seedAdminUser();
        seedInvestigations();
    }

    // ── Tenant ────────────────────────────────────────────────────────────────

    private void seedTenant() {
        if (tenantRepository.existsById(TENANT_ID)) {
            log.debug("[DataSeeder] Tenant already exists — skipping");
            return;
        }
        Tenant tenant = Tenant.builder()
                .id(TENANT_ID)
                .name("University of Tasmania")
                .slug("utas")
                .contactEmail(ADMIN_EMAIL)
                .plan(Tenant.Plan.PROFESSIONAL)
                .creditsRemaining(500)
                .creditsTotal(500)
                .active(true)
                .build();
        tenantRepository.save(tenant);
        log.info("[DataSeeder] Tenant seeded: {}", TENANT_ID);
    }

    // ── Admin User ────────────────────────────────────────────────────────────

    private void seedAdminUser() {
        if (userRepository.existsById(USER_ID)) {
            log.debug("[DataSeeder] Admin user already exists — skipping");
            return;
        }
        User admin = User.builder()
                .id(USER_ID)
                .tenantId(TENANT_ID)
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASS))
                .fullName("Admin User")
                .role(User.Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        log.info("[DataSeeder] Admin user seeded: {}", ADMIN_EMAIL);
    }

    // ── Investigations ────────────────────────────────────────────────────────

    private void seedInvestigations() {
        long count = investigationRepository.countByTenantId(TENANT_ID);
        if (count >= 8) {
            log.debug("[DataSeeder] Investigations already seeded ({}) — skipping", count);
            return;
        }

        LocalDateTime base = LocalDateTime.now().minusHours(2);

        List<Investigation> investigations = List.of(

            // COMPLETED — high confidence (Mitchell — searchable)
            inv("Sarah Mitchell", "1985-03-15",
                "14 Sandy Bay Rd, Hobart TAS 7005", 2007,
                "CSIRO", Investigation.Status.COMPLETED, 82,
                result("sarah.mitchell@gmail.com", "+61412345678",
                       "14 Sandy Bay Rd, Hobart TAS 7005", "CSIRO", "Data Scientist"),
                base.minusMinutes(90)),

            // COMPLETED — medium confidence (Mitchell — searchable)
            inv("James Mitchell", "1990-07-22",
                "Launceston TAS 7250", 2012,
                "Hydro Tasmania", Investigation.Status.COMPLETED, 61,
                result("j.mitchell@outlook.com", null,
                       "Launceston TAS 7250", "Hydro Tasmania", "Civil Engineer"),
                base.minusMinutes(80)),

            // COMPLETED — low confidence
            inv("Emma Thompson", "1992-11-08",
                "Sydney NSW 2000", 2014,
                null, Investigation.Status.COMPLETED, 38,
                result(null, null, "Sydney NSW 2000", null, null),
                base.minusMinutes(70)),

            // COMPLETED — high confidence
            inv("Liam Chen", "1988-05-30",
                "22 Collins St, Melbourne VIC 3000", 2010,
                "Deloitte", Investigation.Status.COMPLETED, 91,
                result("liam.chen@deloitte.com", "+61398765432",
                       "22 Collins St, Melbourne VIC 3000", "Deloitte", "Senior Consultant"),
                base.minusMinutes(60)),

            // PROCESSING
            inv("Olivia Nguyen", "1995-02-14",
                "Brisbane QLD 4000", 2017,
                "Queensland Health", Investigation.Status.PROCESSING, null, null,
                base.minusMinutes(5)),

            // PENDING
            inv("Noah Williams", "1983-09-19",
                "Perth WA 6000", 2005,
                null, Investigation.Status.PENDING, null, null,
                base.minusMinutes(2)),

            // FAILED
            inv("Ava Brown", "1991-12-03",
                "Adelaide SA 5000", 2013,
                null, Investigation.Status.FAILED, null, null,
                base.minusMinutes(50)),

            // COMPLETED — medium confidence
            inv("William Taylor", "1987-06-25",
                "Canberra ACT 2600", 2009,
                "ACT Government", Investigation.Status.COMPLETED, 55,
                result("wtaylor@act.gov.au", null,
                       "Canberra ACT 2600", "ACT Government", "IT Manager"),
                base.minusMinutes(40))
        );

        investigationRepository.saveAll(investigations);
        log.info("[DataSeeder] {} investigations seeded", investigations.size());
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private Investigation inv(
            String name, String dob, String address,
            int gradYear, String employer,
            Investigation.Status status, Integer confidence,
            String resultJson, LocalDateTime createdAt) {

        return Investigation.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(TENANT_ID)
                .createdBy(USER_ID)
                .subjectName(name)
                .subjectDob(dob)
                .subjectLastKnownAddress(address)
                .subjectGraduationYear(gradYear)
                .subjectLastKnownEmployer(employer)
                .status(status)
                .confidenceScore(confidence)
                .resultJson(resultJson)
                .startedAt(status != Investigation.Status.PENDING ? createdAt.plusMinutes(1) : null)
                .completedAt(status == Investigation.Status.COMPLETED
                          || status == Investigation.Status.FAILED
                             ? createdAt.plusMinutes(3) : null)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private String result(String email, String phone, String address,
                          String employer, String jobTitle) {
        return String.format(
            "{\"found_email\":\"%s\",\"found_phone\":\"%s\",\"found_address\":\"%s\"," +
            "\"found_employer\":\"%s\",\"found_job_title\":\"%s\"," +
            "\"confidence_score\":75,\"mode\":\"live\",\"engine\":\"python\"}",
            nvl(email), nvl(phone), nvl(address), nvl(employer), nvl(jobTitle)
        );
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
