package com.project.spring.api.request;

import com.project.spring.data.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier;  // email or phone number

    @NotNull(message = "Channel is required")
    private Channel channel;    // where to send OTP
}
