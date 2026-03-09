package com.project.spring.core.service;

import com.project.spring.data.entity.OtpDeliveryLog;
import com.project.spring.data.entity.OtpSession;
import com.project.spring.data.repository.OtpDeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpDeliveryService {

    private final EmailDeliveryService emailDeliveryService;
    private final SmsDeliveryService smsDeliveryService;
    private final WhatsAppDeliveryService whatsAppDeliveryService;
    private final OtpDeliveryLogRepository deliveryLogRepository;

    /** Delivers OTP via specified channel with 1 automatic retry */
    public boolean deliver(OtpSession session, String otp, String identifier) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                switch (session.getChannel()) {
                    case EMAIL -> emailDeliveryService.send(identifier, otp);
                    case SMS -> smsDeliveryService.send(identifier, otp);
                    case WHATSAPP -> whatsAppDeliveryService.send(identifier, otp);
                }
                saveDeliveryLog(session, OtpDeliveryLog.DeliveryStatus.DELIVERED, attempt, null);
                return true;
            } catch (Exception e) {
                log.error("OTP delivery failed attempt {}: {}", attempt, e.getMessage());
                if (attempt == 2) {
                    saveDeliveryLog(session, OtpDeliveryLog.DeliveryStatus.FAILED, attempt, e.getMessage());
                    return false;
                }
                saveDeliveryLog(session, OtpDeliveryLog.DeliveryStatus.RETRIED, attempt, e.getMessage());
            }
        }
        return false;
    }

    private void saveDeliveryLog(OtpSession session, OtpDeliveryLog.DeliveryStatus status, int attempt, String error) {
        deliveryLogRepository.save(OtpDeliveryLog.builder()
                .otpSession(session)
                .channel(session.getChannel())
                .status(status)
                .attemptNumber(attempt)
                .errorMessage(error)
                .deliveredAt(status == OtpDeliveryLog.DeliveryStatus.DELIVERED ? LocalDateTime.now() : null)
                .build());
    }
}
