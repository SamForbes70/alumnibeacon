package com.alumnibeacon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_queue")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class JobQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "investigation_id", nullable = false)
    private String investigationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 3;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "scheduled_at", nullable = false)
    @Builder.Default
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (this.scheduledAt == null) this.scheduledAt = LocalDateTime.now();
        if (this.status == null) this.status = Status.PENDING;
    }

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }
}
