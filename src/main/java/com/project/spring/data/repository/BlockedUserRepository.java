package com.project.spring.data.repository;

import com.project.spring.data.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    Optional<BlockedUser> findByUser_IdAndActiveTrue(Long userId);
    List<BlockedUser> findAllByActiveTrue();
    boolean existsByUser_IdAndActiveTrue(Long userId);
}
