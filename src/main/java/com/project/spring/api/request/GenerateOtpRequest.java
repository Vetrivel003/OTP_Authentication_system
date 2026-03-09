package com.project.spring.api.request;

import com.project.spring.data.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateOtpRequest {
    @NotBlank(message = "Identifier (email or phone) is required")
    private String identifier;   // email or phone

    @NotNull(message = "Channel is required")
    private Channel channel;
}
