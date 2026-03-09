package com.project.spring.core.service;

import com.project.spring.data.entity.AdminUser;
import com.project.spring.data.entity.AuditLog;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.Channel;
import com.project.spring.data.enums.EventType;
import com.project.spring.data.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, AdminUser admin, EventType eventType, Channel channel,
                    AuditStatus status, String ipAddress, String identifier, String errorMessage) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .admin(admin)
                .eventType(eventType)
                .channel(channel)
                .status(status)
                .ipAddress(ipAddress)
                .identifier(identifier)  // must be pre-masked
                .errorMessage(errorMessage)
                .build();
        auditLogRepository.save(log);
    }

    /** Scheduled cleanup: delete audit logs older than 90 days */
    @Scheduled(cron = "0 0 2 * * *")  // runs daily at 2 AM
    public void purgeOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        auditLogRepository.deleteByCreatedAtBefore(cutoff);
    }
}
