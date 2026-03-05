package com.project.spring.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Generates a cryptographically random 6-digit OTP */
    public String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(900000) + 100000; // 100000–999999
        return String.valueOf(otp);
    }
}
