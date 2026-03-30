package com.alumnibeacon.service;

import com.alumnibeacon.adapter.AugustOsintAdapter;
import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.model.JobQueue;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.JobQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueScheduler {

    private final JobQueueRepository jobQueueRepository;
    private final InvestigationRepository investigationRepository;
    private final AugustOsintAdapter augustAdapter;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void processQueue() {
        List<JobQueue> pending = jobQueueRepository.findPendingJobs();
        if (pending.isEmpty()) return;

        // Process one job at a time to avoid overloading August
        JobQueue job = pending.get(0);
        processJob(job);
    }

    private void processJob(JobQueue job) {
        log.info("Processing job for investigation: {}", job.getInvestigationId());

        // Mark as processing
        job.setStatus(JobQueue.Status.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job.setAttempts(job.getAttempts() + 1);
        jobQueueRepository.save(job);

        // Update investigation status
        investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
            inv.setStatus(Investigation.Status.PROCESSING);
            inv.setStartedAt(LocalDateTime.now());
            investigationRepository.save(inv);
        });

        // Call August OSINT adapter (async)
        augustAdapter.search(job.getPayloadJson())
            .subscribe(
                result -> onSuccess(job, result),
                error -> onFailure(job, error.getMessage())
            );
    }

    @Transactional
    public void onSuccess(JobQueue job, String resultJson) {
        log.info("Job completed for investigation: {}", job.getInvestigationId());
        job.setStatus(JobQueue.Status.COMPLETED);
        job.setResultJson(resultJson);
        job.setCompletedAt(LocalDateTime.now());
        jobQueueRepository.save(job);

        investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
            inv.setStatus(Investigation.Status.COMPLETED);
            inv.setResultJson(resultJson);
            inv.setCompletedAt(LocalDateTime.now());
            // Extract confidence score from result
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(resultJson);
                if (node.has("confidence_score")) {
                    inv.setConfidenceScore(node.get("confidence_score").asInt());
                }
            } catch (Exception e) {
                log.warn("Could not parse confidence score", e);
            }
            investigationRepository.save(inv);
        });
    }

    @Transactional
    public void onFailure(JobQueue job, String errorMessage) {
        log.error("Job failed for investigation: {} - {}", job.getInvestigationId(), errorMessage);
        if (job.getAttempts() >= job.getMaxAttempts()) {
            job.setStatus(JobQueue.Status.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            jobQueueRepository.save(job);

            investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
                inv.setStatus(Investigation.Status.FAILED);
                inv.setErrorMessage(errorMessage);
                investigationRepository.save(inv);
            });
        } else {
            // Reset to pending for retry
            job.setStatus(JobQueue.Status.PENDING);
            job.setErrorMessage(errorMessage);
            jobQueueRepository.save(job);
        }
    }
}
