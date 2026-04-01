package com.alumnibeacon.repository;
import com.alumnibeacon.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);

    java.util.Optional<Tenant> findByStripeSubscriptionId(String stripeSubscriptionId);
}
