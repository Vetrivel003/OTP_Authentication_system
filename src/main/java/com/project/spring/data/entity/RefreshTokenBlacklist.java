package com.project.spring.data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token_blacklist")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenBlacklist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevokeReason reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_admin")
    private AdminUser revokedByAdmin;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at", updatable = false)
    private LocalDateTime revokedAt;

    @PrePersist
    public void prePersist() { revokedAt = LocalDateTime.now(); }

    public enum RevokeReason { LOGOUT, SUSPICIOUS_ACTIVITY, ADMIN_REVOKED }
}
