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

    // Hardcoded COMPLETED count — used for dashboard stats
    @Query("SELECT COUNT(i) FROM Investigation i WHERE i.tenantId = :tenantId AND i.status = 'COMPLETED'")
    long countCompletedByTenantId(@Param("tenantId") String tenantId);

    // Dynamic status count — cast enum to string for String param compatibility (Hibernate 6)
    @Query("SELECT COUNT(i) FROM Investigation i " +
           "WHERE i.tenantId = :tenantId " +
           "AND CAST(i.status AS string) = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") String tenantId,
        @Param("status") String status
    );

    // Filtered list with search + status — cast enum to string for String param compatibility
    @Query("SELECT i FROM Investigation i " +
           "WHERE i.tenantId = :tenantId " +
           "AND (:search = '' OR LOWER(i.subjectName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status = '' OR CAST(i.status AS string) = :status) " +
           "ORDER BY i.createdAt DESC")
    List<Investigation> findByTenantFiltered(
        @Param("tenantId") String tenantId,
        @Param("search") String search,
        @Param("status") String status
    );
}
