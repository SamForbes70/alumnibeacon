package com.alumnibeacon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.STARTER;

    @Column(name = "credits_remaining", nullable = false)
    private int creditsRemaining = 100;

    @Column(name = "credits_total", nullable = false)
    private int creditsTotal = 100;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Plan { STARTER, PROFESSIONAL, ENTERPRISE }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
