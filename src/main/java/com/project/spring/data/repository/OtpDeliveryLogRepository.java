package com.project.spring.data.repository;

import com.project.spring.data.entity.OtpDeliveryLog;
import com.project.spring.data.entity.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtpDeliveryLogRepository extends JpaRepository<OtpDeliveryLog, Long> {

    // Get all delivery attempts for a specific OTP session
    List<OtpDeliveryLog> findByOtpSession(OtpSession otpSession);

    // Get all failed deliveries for a specific session
    List<OtpDeliveryLog> findByOtpSessionAndStatus(
            OtpSession otpSession,
            OtpDeliveryLog.DeliveryStatus status);

    // Count total delivery attempts for a session
    int countByOtpSession(OtpSession otpSession);
}
