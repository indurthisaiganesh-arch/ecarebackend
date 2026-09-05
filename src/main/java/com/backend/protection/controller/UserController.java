package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.user.ChangePasswordRequest;
import com.backend.protection.dto.user.UpdateUserRequest;
import com.backend.protection.dto.user.UserResponse;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile retrieved successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        UserResponse response = userService.updateProfile(principal, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        userService.changePassword(principal, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(@AuthenticationPrincipal UserPrincipal principal) {
        List<UserResponse> users = userService.getAllUsers(principal);
        return ResponseEntity.ok(ApiResponse.success(users, "Users list retrieved"));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleStatus(
            @PathVariable String id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        UserResponse user = userService.toggleUserActive(id, active, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(user, "User status updated"));
    }

    @PutMapping("/{id}/2fa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> toggleMfa(
            @PathVariable String id,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        UserResponse user = userService.toggleUserMfa(id, enabled, principal, httpRequest);
        String msg = enabled ? "Two-factor authentication enabled for user" : "Two-factor authentication disabled for user";
        return ResponseEntity.ok(ApiResponse.success(user, msg));
    }
}
