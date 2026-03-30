package com.alumnibeacon.repository;
import com.alumnibeacon.model.JobQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface JobQueueRepository extends JpaRepository<JobQueue, Long> {
    Optional<JobQueue> findByInvestigationId(String investigationId);

    @Query("SELECT j FROM JobQueue j WHERE j.status = 'PENDING' AND j.attempts < j.maxAttempts ORDER BY j.scheduledAt ASC")
    List<JobQueue> findPendingJobs();

    @Modifying
    @Transactional
    @Query("DELETE FROM JobQueue j WHERE j.investigationId = :investigationId")
    void deleteByInvestigationId(String investigationId);
}
