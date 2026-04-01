package com.alumnibeacon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "investigations")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Investigation {
    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "subject_dob")
    private String subjectDob;

    @Column(name = "subject_last_known_address")
    private String subjectLastKnownAddress;

    @Column(name = "subject_last_known_email")
    private String subjectLastKnownEmail;

    @Column(name = "subject_last_known_phone")
    private String subjectLastKnownPhone;

    @Column(name = "subject_graduation_year")
    private Integer subjectGraduationYear;

    @Column(name = "subject_last_known_employer")
    private String subjectLastKnownEmployer;

    @Column(name = "subject_notes", columnDefinition = "TEXT")
    private String subjectNotes;

    /** Engine preference set at creation time: 'python' (Standard) | 'agent-zero' (Deep) | null (use global config) */
    @Column(name = "preferred_engine")
    private String preferredEngine;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status { PENDING, PROCESSING, COMPLETED, FAILED }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
