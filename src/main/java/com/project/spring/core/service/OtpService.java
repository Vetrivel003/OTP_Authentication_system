package com.project.spring.core.service;

import com.project.spring.config.AppProperties;
import com.project.spring.core.exception.OtpException;
import com.project.spring.core.exception.RateLimitException;
import com.project.spring.core.exception.UserBlockedException;
import com.project.spring.data.entity.BlockedUser;
import com.project.spring.data.entity.OtpSession;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.AuditStatus;
import com.project.spring.data.enums.Channel;
import com.project.spring.data.enums.EventType;
import com.project.spring.data.enums.OtpStatus;
import com.project.spring.data.repository.BlockedUserRepository;
import com.project.spring.data.repository.OtpSessionRepository;
import com.project.spring.data.repository.UserRepository;
import com.project.spring.util.MaskUtil;
import com.project.spring.util.OtpUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final UserRepository userRepository;
    private final OtpSessionRepository otpSessionRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final StringRedisTemplate redisTemplate;
    private final OtpUtil otpUtil;
    private final MaskUtil maskUtil;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final OtpDeliveryService deliveryService;
    private final AuditService auditService;

    @Transactional
    public void generateOtp(String identifier, Channel channel, String ipAddress) {
        User user = findOrCreateUser(identifier);

        if (blockedUserRepository.existsByUser_IdAndActiveTrue(user.getId())) {
            BlockedUser blocked = blockedUserRepository.findByUser_Id(user.getId()).get();
            auditService.log(user, null, EventType.USER_BLOCKED, channel,
                    AuditStatus.BLOCKED, ipAddress, maskUtil.maskIdentifier(identifier), null);
            throw new UserBlockedException(blocked.getReason());
        }

        String rateLimitKey = "rate_limit:" + user.getId();
        String count = redisTemplate.opsForValue().get(rateLimitKey);
        int requestCount = count == null ? 0 : Integer.parseInt(count);

        if (requestCount >= appProperties.getRateLimitCount()) {
            auditService.log(user, null, EventType.RATE_LIMIT_EXCEEDED, channel,
                    AuditStatus.FAILED, ipAddress, maskUtil.maskIdentifier(identifier), null);
            throw new RateLimitException();
        }

        String cooldownKey = "cooldown:" + user.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey))) {
            Long ttl = redisTemplate.getExpire(cooldownKey);
            throw new OtpException(
                    "Please wait " + ttl + " seconds before requesting a new OTP.",
                    "RESEND_COOLDOWN_ACTIVE"
            );
        }

        if (count == null) {
            redisTemplate.opsForValue().set(rateLimitKey, "1",
                    Duration.ofSeconds(appProperties.getRateLimitWindowSeconds()));
        } else {
            redisTemplate.opsForValue().increment(rateLimitKey);
        }

        redisTemplate.opsForValue().set(cooldownKey, "1",
                Duration.ofSeconds(appProperties.getResendCooldownSeconds()));

        String otp = otpUtil.generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        String redisKey = "otp:" + user.getId() + ":" + channel.name();
        redisTemplate.opsForValue().set(redisKey, otpHash,
                Duration.ofSeconds(appProperties.getExpirySeconds()));

        List<OtpSession> activeSessions = otpSessionRepository
                .findByUserAndChannelAndStatusIn(
                        user,
                        channel,
                        List.of(OtpStatus.PENDING, OtpStatus.SENT)
                );

        activeSessions.forEach(oldSession -> {
            oldSession.setStatus(OtpStatus.EXPIRED);
            otpSessionRepository.save(oldSession);
        });

        OtpSession session = OtpSession.builder()
                .user(user)
                .channel(channel)
                .status(OtpStatus.PENDING)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusSeconds(appProperties.getExpirySeconds()))
                .ipAddress(ipAddress)
                .build();
        otpSessionRepository.save(session);

        if (channel == Channel.EMAIL && !identifier.contains("@")) {
            throw new OtpException(
                    "Email address required for EMAIL channel",
                    "INVALID_IDENTIFIER"
            );
        }

        if ((channel == Channel.SMS || channel == Channel.WHATSAPP)
                && identifier.contains("@")) {
            throw new OtpException(
                    "Phone number required for SMS/WHATSAPP channel",
                    "INVALID_IDENTIFIER"
            );
        }

        boolean delivered = deliveryService.deliver(session, otp, identifier);
        session.setStatus(delivered ? OtpStatus.SENT : OtpStatus.FAILED);
        otpSessionRepository.save(session);

        auditService.log(user, null, EventType.OTP_REQUESTED, channel,
                AuditStatus.SUCCESS, ipAddress, maskUtil.maskIdentifier(identifier), null);
    }

    @Transactional
    public Long verifyOtp(String identifier, Channel channel, String otp, String ipAddress) {
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new OtpException("User not found", "OTP_NOT_FOUND"));

        String redisKey = "otp:" + user.getId() + ":" + channel.name();
        String storedHash = redisTemplate.opsForValue().get(redisKey);

        if (storedHash == null) {
            auditService.log(user, null, EventType.OTP_EXPIRED, channel, AuditStatus.EXPIRED, ipAddress, maskUtil.maskIdentifier(identifier), null);
            throw new OtpException("OTP has expired", "OTP_EXPIRED");
        }

        if (!passwordEncoder.matches(otp, storedHash)) {
            auditService.log(user, null, EventType.OTP_INVALID, channel, AuditStatus.FAILED, ipAddress, maskUtil.maskIdentifier(identifier), null);
            throw new OtpException("Invalid OTP", "OTP_INVALID");
        }

        // Delete OTP from Redis immediately after verification
        redisTemplate.delete(redisKey);

        // Mark session verified
        otpSessionRepository.findTopByUserAndChannelAndStatusOrderByCreatedAtDesc(user, channel, OtpStatus.SENT)
                .ifPresent(session -> {
                    session.setStatus(OtpStatus.VERIFIED);
                    session.setVerifiedAt(LocalDateTime.now());
                    session.setUsed(true);
                    otpSessionRepository.save(session);
                });

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        auditService.log(user, null, EventType.OTP_VERIFIED, channel, AuditStatus.SUCCESS, ipAddress, maskUtil.maskIdentifier(identifier), null);

        return user.getId();
    }

    private User findOrCreateUser(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier)
                    .orElseGet(() -> userRepository.save(User.builder().email(identifier).build()));
        } else {
            return userRepository.findByPhoneNumber(identifier)
                    .orElseGet(() -> userRepository.save(User.builder().phoneNumber(identifier).build()));
        }
    }
}
