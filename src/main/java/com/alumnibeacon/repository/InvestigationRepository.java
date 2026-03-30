package com.alumnibeacon.repository;
import com.alumnibeacon.model.Investigation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface InvestigationRepository extends JpaRepository<Investigation, String> {
    List<Investigation> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<Investigation> findByIdAndTenantId(String id, String tenantId);
    long countByTenantId(String tenantId);
    @Query("SELECT COUNT(i) FROM Investigation i WHERE i.tenantId = :tenantId AND i.status = 'COMPLETED'")
    long countCompletedByTenantId(@Param("tenantId") String tenantId);
}
