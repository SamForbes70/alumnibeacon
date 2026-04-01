package com.alumnibeacon.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Returns true if the token is still valid (not expired, not used). */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}
