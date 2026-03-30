package com.alumnibeacon.service;

import com.alumnibeacon.dto.CreateInvestigationRequest;
import com.alumnibeacon.dto.InvestigationDto;
import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.model.JobQueue;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.JobQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
            .status(Investigation.Status.PENDING)
            .build();
        investigationRepository.save(inv);

        // Build payload for August adapter
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "name", req.subjectName(),
                "dob", req.subjectDob() != null ? req.subjectDob() : "",
                "last_known_address", req.subjectLastKnownAddress() != null ? req.subjectLastKnownAddress() : "",
                "last_known_email", req.subjectLastKnownEmail() != null ? req.subjectLastKnownEmail() : "",
                "graduation_year", req.subjectGraduationYear() != null ? req.subjectGraduationYear() : "",
                "last_known_employer", req.subjectLastKnownEmployer() != null ? req.subjectLastKnownEmployer() : "",
                "notes", req.subjectNotes() != null ? req.subjectNotes() : ""
            ));
            JobQueue job = JobQueue.builder()
                .investigationId(inv.getId())
                .tenantId(tenantId)
                .payloadJson(payload)
                .build();
            jobQueueRepository.save(job);
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

    /** Get raw Investigation entity (for PDF generation etc). */
    public Investigation getEntityById(String id, String tenantId) {
        return investigationRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Investigation not found: " + id));
    }

    /** Requeue a failed investigation. */
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

        // Re-queue the job
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "name", inv.getSubjectName(),
                "dob", inv.getSubjectDob() != null ? inv.getSubjectDob() : "",
                "last_known_address", inv.getSubjectLastKnownAddress() != null ? inv.getSubjectLastKnownAddress() : "",
                "last_known_email", inv.getSubjectLastKnownEmail() != null ? inv.getSubjectLastKnownEmail() : "",
                "last_known_employer", inv.getSubjectLastKnownEmployer() != null ? inv.getSubjectLastKnownEmployer() : ""
            ));
            JobQueue job = JobQueue.builder()
                .investigationId(inv.getId())
                .tenantId(tenantId)
                .payloadJson(payload)
                .build();
            jobQueueRepository.save(job);
            log.info("Requeued investigation {}", id);
        } catch (Exception e) {
            log.error("Failed to requeue investigation {}", id, e);
            throw new RuntimeException("Failed to requeue: " + e.getMessage());
        }
    }

    /** Delete an investigation and its associated jobs. */
    @Transactional
    public void delete(String id, String tenantId) {
        Investigation inv = getEntityById(id, tenantId);
        // Delete associated job queue entries
        jobQueueRepository.deleteByInvestigationId(id);
        investigationRepository.delete(inv);
        log.info("Deleted investigation {} for tenant {}", id, tenantId);
    }

}
