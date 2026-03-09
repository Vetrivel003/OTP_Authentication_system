package com.project.spring.data.repository;

import com.project.spring.data.entity.RefreshTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenBlacklistRepository extends JpaRepository<RefreshTokenBlacklist, Long> {
    boolean existsByTokenHash(String tokenHash);
}
