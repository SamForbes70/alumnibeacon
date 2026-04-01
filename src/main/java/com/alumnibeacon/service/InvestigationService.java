package com.alumnibeacon.service;

import com.alumnibeacon.dto.CreateInvestigationRequest;
import com.alumnibeacon.dto.InvestigationDto;
import com.alumnibeacon.dto.OsintResultDto;
import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.model.JobQueue;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.JobQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final JobQueueRepository jobQueueRepository;
    private final ObjectMapper objectMapper;

    public List<InvestigationDto> listByTenant(String tenantId) {
        return investigationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream().map(InvestigationDto::from).toList();
    }

    public InvestigationDto getById(String id, String tenantId) {
        return investigationRepository.findByIdAndTenantId(id, tenantId)
            .map(InvestigationDto::from)
            .orElseThrow(() -> new RuntimeException("Investigation not found"));
    }

    @Transactional
    public InvestigationDto create(CreateInvestigationRequest req, String tenantId, String userId) {
        // Normalise engine preference — default to 'python' if not specified
        String engine = (req.preferredEngine() != null && !req.preferredEngine().isBlank())
            ? req.preferredEngine().trim().toLowerCase()
            : "python";

        Investigation inv = Investigation.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .createdBy(userId)
            .subjectName(req.subjectName())
            .subjectDob(req.subjectDob())
            .subjectLastKnownAddress(req.subjectLastKnownAddress())
            .subjectLastKnownEmail(req.subjectLastKnownEmail())
            .subjectLastKnownPhone(req.subjectLastKnownPhone())
            .subjectGraduationYear(req.subjectGraduationYear())
            .subjectLastKnownEmployer(req.subjectLastKnownEmployer())
            .subjectNotes(req.subjectNotes())
            .preferredEngine(engine)
            .status(Investigation.Status.PENDING)
            .build();
        investigationRepository.save(inv);

        // Build payload for OSINT adapter — include preferred_engine so scheduler can route correctly
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("name",                req.subjectName());
            payloadMap.put("dob",                 req.subjectDob() != null ? req.subjectDob() : "");
            payloadMap.put("last_known_address",  req.subjectLastKnownAddress() != null ? req.subjectLastKnownAddress() : "");
            payloadMap.put("last_known_email",    req.subjectLastKnownEmail() != null ? req.subjectLastKnownEmail() : "");
            payloadMap.put("graduation_year",     req.subjectGraduationYear() != null ? req.subjectGraduationYear() : "");
            payloadMap.put("last_known_employer", req.subjectLastKnownEmployer() != null ? req.subjectLastKnownEmployer() : "");
            payloadMap.put("notes",               req.subjectNotes() != null ? req.subjectNotes() : "");
            payloadMap.put("preferred_engine",    engine);

            String payload = objectMapper.writeValueAsString(payloadMap);
            JobQueue job = JobQueue.builder()
                .investigationId(inv.getId())
                .tenantId(tenantId)
                .payloadJson(payload)
                .build();
            jobQueueRepository.save(job);
            log.info("Queued investigation {} with engine={}", inv.getId(), engine);
        } catch (Exception e) {
            log.error("Failed to queue investigation {}", inv.getId(), e);
        }

        return InvestigationDto.from(inv);
    }

    public Map<String, Object> getDashboardStats(String tenantId) {
        long total = investigationRepository.countByTenantId(tenantId);
        long completed = investigationRepository.countCompletedByTenantId(tenantId);
        double successRate = total > 0 ? (double) completed / total * 100 : 0;
        return Map.of(
            "total", total,
            "completed", completed,
            "successRate", Math.round(successRate)
        );
    }

    public List<InvestigationDto> listByTenantFiltered(String tenantId, String search, String status) {
        return investigationRepository
            .findByTenantFiltered(
                tenantId,
                search != null ? search.trim() : "",
                status != null ? status.trim() : "")
            .stream().map(InvestigationDto::from).toList();
    }

    public long countByTenant(String tenantId) {
        return investigationRepository.countByTenantId(tenantId);
    }

    public long countByTenantAndStatus(String tenantId, String status) {
        return investigationRepository.countByTenantIdAndStatus(tenantId, status);
    }

    /** Get raw Investigation entity (for PDF generation etc). */
    public Investigation getEntityById(String id, String tenantId) {
        return investigationRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Investigation not found: " + id));
    }

    /** Requeue a failed investigation, preserving original engine preference. */
    @Transactional
    public void retry(String id, String tenantId) {
        Investigation inv = getEntityById(id, tenantId);
        if (inv.getStatus() != Investigation.Status.FAILED) {
            throw new RuntimeException("Only FAILED investigations can be retried");
        }
        inv.setStatus(Investigation.Status.PENDING);
        inv.setErrorMessage(null);
        inv.setStartedAt(null);
        inv.setCompletedAt(null);
        investigationRepository.save(inv);

        // Re-queue preserving original engine preference
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("name",                inv.getSubjectName());
            payloadMap.put("dob",                 inv.getSubjectDob() != null ? inv.getSubjectDob() : "");
            payloadMap.put("last_known_address",  inv.getSubjectLastKnownAddress() != null ? inv.getSubjectLastKnownAddress() : "");
            payloadMap.put("last_known_email",    inv.getSubjectLastKnownEmail() != null ? inv.getSubjectLastKnownEmail() : "");
            payloadMap.put("last_known_employer", inv.getSubjectLastKnownEmployer() != null ? inv.getSubjectLastKnownEmployer() : "");
            payloadMap.put("preferred_engine",    inv.getPreferredEngine() != null ? inv.getPreferredEngine() : "python");

            String payload = objectMapper.writeValueAsString(payloadMap);
            JobQueue job = JobQueue.builder()
                .investigationId(inv.getId())
                .tenantId(tenantId)
                .payloadJson(payload)
                .build();
            jobQueueRepository.save(job);
            log.info("Requeued investigation {} with engine={}", id, inv.getPreferredEngine());
        } catch (Exception e) {
            log.error("Failed to requeue investigation {}", id, e);
            throw new RuntimeException("Failed to requeue: " + e.getMessage());
        }
    }

    /** Delete an investigation and its associated jobs. */
    @Transactional
    public void delete(String id, String tenantId) {
        Investigation inv = getEntityById(id, tenantId);
        jobQueueRepository.deleteByInvestigationId(id);
        investigationRepository.delete(inv);
        log.info("Deleted investigation {} for tenant {}", id, tenantId);
    }

    /**
     * Parse the raw resultJson string into a structured OsintResultDto.
     * Returns null if resultJson is null/blank or cannot be parsed.
     */
    public OsintResultDto parseOsintResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) return null;
        try {
            return objectMapper.readValue(resultJson, OsintResultDto.class);
        } catch (Exception e) {
            log.warn("Could not parse OSINT result JSON: {}", e.getMessage());
            return null;
        }
    }
}
