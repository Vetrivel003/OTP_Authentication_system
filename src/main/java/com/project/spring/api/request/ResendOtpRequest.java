package com.project.spring.api.request;

import com.project.spring.data.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResendOtpRequest {
    @NotBlank
    private String identifier;
    @NotNull
    private Channel channel;
}
