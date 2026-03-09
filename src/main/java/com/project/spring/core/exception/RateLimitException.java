package com.project.spring.core.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException() {
        super("Rate limit exceeded. Maximum 5 OTP requests per hour.");
    }
}
