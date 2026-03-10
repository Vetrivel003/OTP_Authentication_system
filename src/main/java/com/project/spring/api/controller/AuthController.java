package com.project.spring.api.controller;

import com.project.spring.api.request.LoginRequest;
import com.project.spring.api.request.RefreshTokenRequest;
import com.project.spring.api.request.RegisterRequest;
import com.project.spring.api.response.ApiResponse;
import com.project.spring.api.response.RegisterResponse;
import com.project.spring.api.response.VerifyOtpResponse;
import com.project.spring.core.service.AuthService;
import com.project.spring.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        RegisterResponse response = authService.register(request, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        authService.login(request, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(
                "OTP sent to your " + request.getChannel().name().toLowerCase(), null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        VerifyOtpResponse response = authService.refreshToken(request.getRefreshToken(), getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal Long userId,
            HttpServletRequest httpRequest) {
        authService.logout(request.getRefreshToken(), userId, getIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = jwtUtil.isTokenValid(token);
        return ResponseEntity.ok(ApiResponse.success("Token validation result", valid));
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return ip != null ? ip.split(",")[0].trim() : request.getRemoteAddr();
    }
}
