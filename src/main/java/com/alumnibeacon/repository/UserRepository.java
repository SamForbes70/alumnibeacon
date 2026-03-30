package com.alumnibeacon.repository;
import com.alumnibeacon.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByTenantIdAndEmail(String tenantId, String email);
    Optional<User> findByEmail(String email);
    List<User> findByTenantId(String tenantId);
    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
