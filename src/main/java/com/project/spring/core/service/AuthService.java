package com.project.spring.core.service;

import com.project.spring.api.request.LoginRequest;
import com.project.spring.api.request.RegisterRequest;
import com.project.spring.api.response.RegisterResponse;
import com.project.spring.api.response.VerifyOtpResponse;
import com.project.spring.core.exception.OtpException;
import com.project.spring.data.entity.RefreshTokenBlacklist;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.EventType;
import com.project.spring.data.repository.RefreshTokenBlacklistRepository;
import com.project.spring.data.repository.UserRepository;
import com.project.spring.security.JwtUtil;
import com.project.spring.util.MaskUtil;
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
    private final OtpService otpService;
    private final MaskUtil maskUtil;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    @Transactional
    public RegisterResponse register(RegisterRequest request, String ipAddress) {

        if (request.getEmail() == null && request.getPhoneNumber() == null) {
            throw new OtpException(
                    "Either email or phone number is required",
                    "IDENTIFIER_MISSING");
        }

        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new OtpException(
                    "Email already registered. Please login instead.",
                    "EMAIL_ALREADY_EXISTS");
        }

        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new OtpException(
                    "Phone number already registered. Please login instead.",
                    "PHONE_ALREADY_EXISTS");
        }

        User user = User.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .active(true)
                .verified(false)
                .build();
        userRepository.save(user);

        String identifier = request.getChannel().equals("EMAIL")
                ? request.getEmail()
                : request.getPhoneNumber();

        otpService.generateOtp(identifier, request.getChannel(), ipAddress);

        return RegisterResponse.builder()
                .userId(user.getId())
                .maskedIdentifier(maskUtil.maskIdentifier(identifier))
                .message("OTP sent to " + maskUtil.maskIdentifier(identifier))
                .build();
    }

    public void login(LoginRequest request, String ipAddress) {

        boolean exists = request.getIdentifier().contains("@")
                ? userRepository.existsByEmail(request.getIdentifier())
                : userRepository.existsByPhoneNumber(request.getIdentifier());

        if (!exists) {
            throw new OtpException(
                    "No account found. Please register first.",
                    "USER_NOT_FOUND");
        }

        otpService.generateOtp(
                request.getIdentifier(),
                request.getChannel(),
                ipAddress);
    }

    public VerifyOtpResponse issueTokens(Long userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OtpException("User not found", "USER_NOT_FOUND"));

        String accessToken = jwtUtil.generateAccessToken(userId, "USER");
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        auditService.log(user, null, EventType.TOKEN_ISSUED,
                null, AuditStatus.SUCCESS, ipAddress, null, null);

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

        if (userId == null) {
            throw new OtpException("User not authenticated", "UNAUTHORIZED");
        }

        String tokenHash = hashToken(refreshToken);
        User user = userRepository.findById(userId).orElseThrow();

        blacklistRepository.save(RefreshTokenBlacklist.builder()
                .user(user)
                .tokenHash(tokenHash)
                .reason(RefreshTokenBlacklist.RevokeReason.LOGOUT)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        auditService.log(user, null, EventType.TOKEN_REVOKED,
                null, AuditStatus.SUCCESS, ipAddress, null, null);
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