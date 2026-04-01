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

    @Column(name = "contact_email")
    private String contactEmail;


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Plan plan = Plan.STARTER;

    @Column(name = "credits_remaining", nullable = false)
    @Builder.Default
    private int creditsRemaining = 100;

    @Column(name = "credits_total", nullable = false)
    @Builder.Default
    private int creditsTotal = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();


    @Column(name = "subscription_status", nullable = false)
    @Builder.Default
    private String subscriptionStatus = "trialing";

    @Column(name = "subscription_period_end")
    private String subscriptionPeriodEnd;

    @Column(name = "monthly_investigation_limit", nullable = false)
    @Builder.Default
    private int monthlyInvestigationLimit = 10;

    @Column(name = "investigations_used_this_month", nullable = false)
    @Builder.Default
    private int investigationsUsedThisMonth = 0;

    @Column(name = "billing_cycle_anchor")
    private String billingCycleAnchor;
    public enum Plan { STARTER, PROFESSIONAL, ENTERPRISE }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
