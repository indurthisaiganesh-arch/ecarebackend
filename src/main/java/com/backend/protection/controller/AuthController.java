package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.auth.*;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> result = authService.register(request, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.created(result, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        Map<String, Object> result = authService.initiateLogin(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result, "OTP sent for verification"));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(
            @RequestParam String email,
            @RequestParam(defaultValue = "LOGIN") String purpose,
            HttpServletRequest httpRequest) {
        Map<String, Object> result = authService.sendOtp(email, purpose, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result, "OTP sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody OtpRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.verifyOtpAndLogin(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam String refreshToken,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshTokens(refreshToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Tokens refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
