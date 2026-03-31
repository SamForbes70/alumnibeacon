package com.alumnibeacon.repository;

import com.alumnibeacon.model.JobQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobQueueRepository extends JpaRepository<JobQueue, Long> {

    Optional<JobQueue> findByInvestigationId(String investigationId);

    /** Pick up next PENDING jobs ordered by scheduled time */
    @Query("SELECT j FROM JobQueue j WHERE j.status = 'PENDING' AND j.attempts < j.maxAttempts ORDER BY j.scheduledAt ASC")
    List<JobQueue> findPendingJobs();

    /** Find jobs stuck in PROCESSING state beyond the threshold time */
    @Query("SELECT j FROM JobQueue j WHERE j.status = 'PROCESSING' AND j.startedAt < :threshold")
    List<JobQueue> findStuckJobs(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobQueue j WHERE j.investigationId = :investigationId")
    void deleteByInvestigationId(String investigationId);
}
