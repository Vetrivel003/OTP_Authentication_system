package com.project.spring.data.repository;

import com.project.spring.data.entity.AuditLog;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUser_IdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<AuditLog> findByStatus(AuditStatus status, Pageable pageable);
    long countByEventType(EventType eventType);
    long countByStatus(AuditStatus status);
    void deleteByCreatedAtBefore(LocalDateTime cutoff);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.eventType = 'TOKEN_ISSUED'")
    long countActiveSessions();
}
