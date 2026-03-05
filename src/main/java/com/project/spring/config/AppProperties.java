package com.project.spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "otp")
@Getter
@Setter
public class AppProperties {
    private int expirySeconds = 300;
    private int rateLimitCount = 5;
    private int rateLimitWindowSeconds = 3600;
    private int resendCooldownSeconds = 30;
}