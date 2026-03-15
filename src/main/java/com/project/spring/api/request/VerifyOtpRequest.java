package com.project.spring.api.request;

import com.project.spring.data.enums.Channel;
import com.project.spring.data.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank
    private String identifier;

    @NotNull
    private Channel channel;

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
