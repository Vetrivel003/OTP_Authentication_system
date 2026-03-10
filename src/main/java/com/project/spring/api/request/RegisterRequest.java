package com.project.spring.api.request;

import com.project.spring.data.enums.Channel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {

    // Either email or phone must be provided — validated in service
    private String email;
    private String phoneNumber;

    @NotNull(message = "Channel is required")
    private Channel channel;  // preferred OTP delivery channel
}
