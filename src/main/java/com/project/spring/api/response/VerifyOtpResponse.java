package com.project.spring.api.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
}
