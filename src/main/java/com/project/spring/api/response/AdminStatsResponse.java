package com.project.spring.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalRequests;
    private long successCount;
    private long failedDeliveries;
    private long activeSessions;
    private double successRate;
}
