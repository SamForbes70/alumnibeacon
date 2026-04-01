package com.alumnibeacon.service;

import com.alumnibeacon.adapter.OsintAdapterRouter;
import com.alumnibeacon.model.Investigation;
import com.alumnibeacon.model.JobQueue;
import com.alumnibeacon.repository.InvestigationRepository;
import com.alumnibeacon.repository.JobQueueRepository;
import com.alumnibeacon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueScheduler {

    private final JobQueueRepository    jobQueueRepository;
    private final InvestigationRepository investigationRepository;
    private final OsintAdapterRouter    osintRouter;
    private final EmailService          emailService;
    private final UserRepository        userRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    /**
     * Main queue processor - runs every 5 seconds.
     * Uses synchronous .block() so @Transactional works correctly
     * on the Spring-managed scheduler thread.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processQueue() {
        // Step 1: Recover any jobs stuck in PROCESSING
        // Threshold is 20 min to accommodate Agent Zero deep investigations (~15 min)
        recoverStuckJobs();

        // Step 2: Pick up next PENDING job
        List<JobQueue> pending = jobQueueRepository.findPendingJobs();
        if (pending.isEmpty()) return;

        JobQueue job = pending.get(0);
        log.info("Processing job {} for investigation: {} (engine={})",
            job.getId(), job.getInvestigationId(), osintRouter.getEngine());
        processJobSynchronously(job);
    }

    /**
     * Reset jobs stuck in PROCESSING back to PENDING.
     * Threshold: 20 minutes — accommodates Agent Zero deep investigations (~15 min).
     */
    private void recoverStuckJobs() {
        LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(20);
        List<JobQueue> stuckJobs = jobQueueRepository.findStuckJobs(stuckThreshold);
        if (!stuckJobs.isEmpty()) {
            log.warn("Found {} stuck job(s) - resetting to PENDING", stuckJobs.size());
            for (JobQueue stuck : stuckJobs) {
                if (stuck.getAttempts() >= stuck.getMaxAttempts()) {
                    stuck.setStatus(JobQueue.Status.FAILED);
                    stuck.setErrorMessage("Max retry attempts exceeded - job timed out");
                    stuck.setCompletedAt(LocalDateTime.now());
                    jobQueueRepository.save(stuck);

                    investigationRepository.findById(stuck.getInvestigationId()).ifPresent(inv -> {
                        inv.setStatus(Investigation.Status.FAILED);
                        inv.setErrorMessage("Search timed out after " + stuck.getAttempts() + " attempts");
                        investigationRepository.save(inv);
                        sendFailureEmail(inv, "Search timed out after " + stuck.getAttempts() + " attempts");
                    });
                    log.error("Job {} failed after {} attempts", stuck.getId(), stuck.getAttempts());
                } else {
                    stuck.setStatus(JobQueue.Status.PENDING);
                    stuck.setStartedAt(null);
                    jobQueueRepository.save(stuck);

                    investigationRepository.findById(stuck.getInvestigationId()).ifPresent(inv -> {
                        inv.setStatus(Investigation.Status.PENDING);
                        inv.setStartedAt(null);
                        investigationRepository.save(inv);
                    });
                    log.info("Reset stuck job {} to PENDING (attempt {}/{})",
                        stuck.getId(), stuck.getAttempts(), stuck.getMaxAttempts());
                }
            }
        }
    }

    /**
     * Process a job SYNCHRONOUSLY using .block().
     *
     * WHY .block() instead of .subscribe():
     * - .subscribe() runs callbacks on Reactor threads
     * - @Transactional uses ThreadLocal - doesn't propagate to Reactor threads
     * - Result: DB saves in callbacks silently fail, job stays PROCESSING forever
     * - .block() keeps execution on the Spring scheduler thread where @Transactional works
     */
    @Transactional
    public void processJobSynchronously(JobQueue job) {
        // Mark as PROCESSING
        job.setStatus(JobQueue.Status.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        job.setAttempts(job.getAttempts() + 1);
        jobQueueRepository.save(job);

        investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
            inv.setStatus(Investigation.Status.PROCESSING);
            inv.setStartedAt(LocalDateTime.now());
            investigationRepository.save(inv);
        });

        try {
            log.info("Routing OSINT investigation {} via {} engine (attempt {}/{})",
                job.getInvestigationId(), osintRouter.getEngine(),
                job.getAttempts(), job.getMaxAttempts());

            String resultJson = osintRouter.route(job.getPayloadJson()).block();

            if (resultJson == null || resultJson.isBlank()) {
                throw new RuntimeException("OSINT router returned empty response");
            }

            // SUCCESS
            log.info("OSINT investigation completed for {} (engine={})",
                job.getInvestigationId(), osintRouter.getEngine());
            job.setStatus(JobQueue.Status.COMPLETED);
            job.setResultJson(resultJson);
            job.setCompletedAt(LocalDateTime.now());
            jobQueueRepository.save(job);

            investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
                inv.setStatus(Investigation.Status.COMPLETED);
                inv.setResultJson(resultJson);
                inv.setCompletedAt(LocalDateTime.now());

                // Extract confidence score and engine from result
                String engine = osintRouter.getEngine();
                try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    var node   = mapper.readTree(resultJson);
                    if (node.has("confidence_score")) {
                        inv.setConfidenceScore(node.get("confidence_score").asInt());
                    }
                    if (node.has("engine")) {
                        engine = node.get("engine").asText();
                        log.info("Investigation {} completed via engine: {}", inv.getId(), engine);
                    }
                } catch (Exception e) {
                    log.warn("Could not parse result metadata", e);
                }
                investigationRepository.save(inv);

                // P4 — Send completion email notification
                sendCompletionEmail(inv, engine);
            });

        } catch (Exception e) {
            log.error("OSINT investigation failed for {}: {}",
                job.getInvestigationId(), e.getMessage());

            if (job.getAttempts() >= job.getMaxAttempts()) {
                // Final failure
                job.setStatus(JobQueue.Status.FAILED);
                job.setErrorMessage(e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                jobQueueRepository.save(job);

                investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
                    inv.setStatus(Investigation.Status.FAILED);
                    inv.setErrorMessage(e.getMessage());
                    investigationRepository.save(inv);

                    // P4 — Send failure email notification
                    sendFailureEmail(inv, e.getMessage());
                });
            } else {
                // Retry - reset to PENDING
                job.setStatus(JobQueue.Status.PENDING);
                job.setErrorMessage(e.getMessage());
                job.setStartedAt(null);
                jobQueueRepository.save(job);

                investigationRepository.findById(job.getInvestigationId()).ifPresent(inv -> {
                    inv.setStatus(Investigation.Status.PENDING);
                    investigationRepository.save(inv);
                });
                log.info("Job {} reset to PENDING for retry ({}/{})",
                    job.getId(), job.getAttempts(), job.getMaxAttempts());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // P4 — Email helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Sends an investigation-completion email to the user who submitted it.
     * Silently skips if the user cannot be found.
     */
    private void sendCompletionEmail(Investigation inv, String engine) {
        try {
            userRepository.findById(inv.getCreatedBy()).ifPresent(user ->
                emailService.sendInvestigationComplete(
                    user.getEmail(),
                    user.getFullName(),
                    inv.getSubjectName(),
                    inv.getId(),
                    inv.getConfidenceScore() != null ? inv.getConfidenceScore() : 0,
                    engine,
                    appBaseUrl
                )
            );
        } catch (Exception e) {
            // Email failure must never break the investigation flow
            log.warn("Could not send completion email for investigation {}: {}",
                inv.getId(), e.getMessage());
        }
    }

    /**
     * Sends an investigation-failure email to the user who submitted it.
     * Silently skips if the user cannot be found.
     */
    private void sendFailureEmail(Investigation inv, String errorMessage) {
        try {
            userRepository.findById(inv.getCreatedBy()).ifPresent(user ->
                emailService.sendInvestigationFailed(
                    user.getEmail(),
                    user.getFullName(),
                    inv.getSubjectName(),
                    inv.getId(),
                    errorMessage != null ? errorMessage : "Unknown error",
                    appBaseUrl
                )
            );
        } catch (Exception e) {
            log.warn("Could not send failure email for investigation {}: {}",
                inv.getId(), e.getMessage());
        }
    }
}
