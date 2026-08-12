package com.charity.app.model;

import com.charity.app.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "full_name")
    private String fullName;

    @Builder.Default
    private boolean enabled = true;

    /**
     * Consecutive failed sign-ins. Persisted rather than held in memory so a restart cannot be used
     * to reset the counter, and so it is shared by both backend containers.
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    /**
     * When the temporary lock lifts. Expiry is lazy -- a timestamp in the past simply reads as
     * unlocked, so no scheduled job is needed.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
