package com.project.spring.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlockUserRequest {
    @NotNull
    private Long userId;
    @NotBlank
    private String reason;
}
