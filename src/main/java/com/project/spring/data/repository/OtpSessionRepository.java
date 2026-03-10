package com.project.spring.data.repository;

import com.project.spring.data.entity.OtpSession;
import com.project.spring.data.entity.User;
import com.project.spring.data.enums.Channel;
import com.project.spring.data.enums.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {
    Optional<OtpSession> findTopByUserAndChannelAndStatusOrderByCreatedAtDesc(
            User user, Channel channel, OtpStatus status);
    long countByStatus(OtpStatus status);
    // Find any active (PENDING or SENT) session for this user and channel
    List<OtpSession> findByUserAndChannelAndStatusIn(User user, Channel channel, List<OtpStatus> statuses);
}
