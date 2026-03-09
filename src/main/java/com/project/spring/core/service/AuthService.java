package com.project.spring.core.service;

import com.project.spring.api.response.VerifyOtpResponse;
import com.project.spring.core.exception.OtpException;
import com.project.spring.data.entity.RefreshTokenBlacklist;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.EventType;
import com.project.spring.data.repository.RefreshTokenBlacklistRepository;
import com.project.spring.data.repository.UserRepository;
import com.project.spring.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenBlacklistRepository blacklistRepository;
    private final AuditService auditService;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    public VerifyOtpResponse issueTokens(Long userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OtpException("User not found", "USER_NOT_FOUND"));

        String accessToken = jwtUtil.generateAccessToken(userId, "USER");
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        auditService.log(user, null, EventType.TOKEN_ISSUED, null, AuditStatus.SUCCESS, ipAddress, null, null);

        return VerifyOtpResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(900)
                .build();
    }

    @Transactional
    public VerifyOtpResponse refreshToken(String refreshToken, String ipAddress) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new OtpException("Invalid or expired refresh token", "TOKEN_INVALID");
        }

        String tokenHash = hashToken(refreshToken);
        if (blacklistRepository.existsByTokenHash(tokenHash)) {
            throw new OtpException("Token has been revoked", "TOKEN_REVOKED");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OtpException("User not found", "USER_NOT_FOUND"));

        // Blacklist old refresh token (rotation)
        blacklistRepository.save(RefreshTokenBlacklist.builder()
                .user(user)
                .tokenHash(tokenHash)
                .reason(RefreshTokenBlacklist.RevokeReason.LOGOUT)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiry / 1000))
                .build());

        String newAccessToken = jwtUtil.generateAccessToken(userId, "USER");
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);

        return VerifyOtpResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(900)
                .build();
    }

    @Transactional
    public void logout(String refreshToken, Long userId, String ipAddress) {
        String tokenHash = hashToken(refreshToken);
        User user = userRepository.findById(userId).orElseThrow();

        blacklistRepository.save(RefreshTokenBlacklist.builder()
                .user(user)
                .tokenHash(tokenHash)
                .reason(RefreshTokenBlacklist.RevokeReason.LOGOUT)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        auditService.log(user, null, EventType.TOKEN_REVOKED, null, AuditStatus.SUCCESS, ipAddress, null, null);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Token hashing failed", e);
        }
    }
}