package com.project.spring.api.controller;

import com.project.spring.api.request.GenerateOtpRequest;
import com.project.spring.api.request.ResendOtpRequest;
import com.project.spring.api.request.VerifyOtpRequest;
import com.project.spring.api.response.ApiResponse;
import com.project.spring.api.response.VerifyOtpResponse;
import com.project.spring.core.service.AuthService;
import com.project.spring.core.service.OtpService;
import com.project.spring.data.enums.OtpPurpose;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final AuthService authService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Void>> generate(
            @Valid @RequestBody GenerateOtpRequest request,
            HttpServletRequest httpRequest) {
        otpService.generateOtp(request.getIdentifier(), request.getChannel(), getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<Void>> resend(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {
        otpService.generateOtp(request.getIdentifier(), request.getChannel(), getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verify(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        String ip = getIp(httpRequest);

        // Verify OTP and get the userId from service
        Long userId = otpService.verifyOtp(
                request.getIdentifier(),
                request.getChannel(),
                request.getOtp(),
                ip
        );

        if(request.getPurpose() == OtpPurpose.REGISTER){
            return ResponseEntity.ok(
                    ApiResponse.success("Registration verified. Please login to continue.", null)
            );
        }

        // Issue authentication tokens
        VerifyOtpResponse tokens = authService.issueTokens(userId, ip);
        return ResponseEntity.ok(
                ApiResponse.success("OTP verified successfully", tokens)
        );
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }

}
