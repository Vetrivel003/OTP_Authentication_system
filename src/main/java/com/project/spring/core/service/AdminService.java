package com.project.spring.core.service;

import com.project.spring.api.request.BlockUserRequest;
import com.project.spring.api.response.AdminStatsResponse;
import com.project.spring.core.exception.OtpException;
import com.project.spring.data.entity.AdminUser;
import com.project.spring.data.entity.BlockedUser;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.EventType;
import com.project.spring.data.enums.OtpStatus;
import com.project.spring.data.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final AuditLogRepository auditLogRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final AuditService auditService;
    private final AdminRepository adminRepository;

    @Transactional
    public void blockUser(BlockUserRequest request, Long adminId) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new OtpException("User not found", "USER_NOT_FOUND"));

        AdminUser admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new OtpException("Admin not found", "ADMIN_NOT_FOUND"));

        if (blockedUserRepository.existsByUser_IdAndActiveTrue(user.getId())) {
            throw new OtpException("User is already blocked", "ALREADY_BLOCKED");
        }

        blockedUserRepository.save(BlockedUser.builder()
                .user(user)
                .reason(request.getReason())
                .build());

        auditService.log(user, admin, EventType.USER_BLOCKED, null, AuditStatus.SUCCESS, null, null, null);
    }

    @Transactional
    public void unblockUser(Long userId, Long adminId) {
        BlockedUser blocked = blockedUserRepository.findByUser_IdAndActiveTrue(userId)
                .orElseThrow(() -> new OtpException("User is not blocked", "USER_NOT_BLOCKED"));

        blocked.setActive(false);
        blockedUserRepository.save(blocked);

        AdminUser admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new OtpException("Admin not found", "ADMIN_NOT_FOUND"));

        auditService.log(blocked.getUser(), admin, EventType.USER_UNBLOCKED, null, AuditStatus.SUCCESS, null, null, null);
    }

    public List<BlockedUser> getBlockedUsers() {
        return blockedUserRepository.findAllByActiveTrue();
    }

    public AdminStatsResponse getStats() {
        long total = auditLogRepository.count();
        long success = auditLogRepository.countByStatus(AuditStatus.SUCCESS);
        long failed = auditLogRepository.countByStatus(AuditStatus.FAILED);
        long activeSessions = otpSessionRepository.countByStatus(OtpStatus.SENT);
        double successRate = total > 0 ? (double) success / total * 100 : 0;

        return AdminStatsResponse.builder()
                .totalRequests(total)
                .successCount(success)
                .failedDeliveries(failed)
                .activeSessions(activeSessions)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .build();
    }
}
