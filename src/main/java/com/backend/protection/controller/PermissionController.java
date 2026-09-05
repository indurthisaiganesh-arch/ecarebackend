package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.permission.GrantPermissionRequest;
import com.backend.protection.dto.permission.PermissionResponse;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PermissionResponse>> grantPermission(
            @Valid @RequestBody GrantPermissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        PermissionResponse response = permissionService.grantPermission(request, principal, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.created(response, "Access permission / consent granted successfully"));
    }

    @PutMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revokePermission(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        permissionService.revokePermission(id, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Permission revoked successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        permissionService.revokePermission(id, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Permission revoked"));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissionsForPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PermissionResponse> list = permissionService.getPermissionsForPatient(patientId, principal);
        return ResponseEntity.ok(ApiResponse.success(list, "Permissions retrieved"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<PermissionResponse> list = permissionService.getAllPermissions(principal);
        return ResponseEntity.ok(ApiResponse.success(list, "All permissions retrieved"));
    }
}
