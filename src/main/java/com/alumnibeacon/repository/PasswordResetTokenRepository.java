package com.alumnibeacon.repository;

import com.alumnibeacon.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByToken(String token);

    /** Invalidate all existing unused tokens for a user before issuing a new one. */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.userId = :userId AND t.used = false")
    void invalidateAllForUser(String userId);

    /** Clean up expired tokens older than 24 hours. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(LocalDateTime cutoff);
}
