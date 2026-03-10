package com.project.spring.api.controller;

import com.project.spring.api.request.AdminLoginRequest;
import com.project.spring.api.request.BlockUserRequest;
import com.project.spring.api.response.AdminStatsResponse;
import com.project.spring.api.response.ApiResponse;
import com.project.spring.api.response.VerifyOtpResponse;
import com.project.spring.core.service.AdminService;
import com.project.spring.data.entity.AuditLog;
import com.project.spring.data.entity.BlockedUser;
import com.project.spring.data.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditLogRepository auditLogRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest) {
        VerifyOtpResponse response = adminService.login(request, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Admin login successful", response));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLogs(Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched", logs));
    }

    @GetMapping("/logs/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", adminService.getStats()));
    }

    @PostMapping("/users/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @Valid @RequestBody BlockUserRequest request,
            @AuthenticationPrincipal Long adminId) {
        adminService.blockUser(request, adminId);
        return ResponseEntity.ok(ApiResponse.success("User blocked", null));
    }

    @PostMapping("/users/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @RequestParam Long userId,
            @AuthenticationPrincipal Long adminId) {
        adminService.unblockUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked", null));
    }

    @GetMapping("/users/blocked")
    public ResponseEntity<ApiResponse<List<BlockedUser>>> getBlockedUsers() {
        return ResponseEntity.ok(ApiResponse.success("Blocked users fetched", adminService.getBlockedUsers()));
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
