package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.staff.AppointStaffRequest;
import com.backend.protection.dto.user.UserResponse;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.StaffRecruitmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
public class StaffRecruitmentController {

    private final StaffRecruitmentService recruitmentService;

    public StaffRecruitmentController(StaffRecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @PostMapping("/appoint")
    @PreAuthorize("hasAnyRole('HEAD_RECRUITER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> appointStaff(
            @Valid @RequestBody AppointStaffRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        UserResponse appointed = recruitmentService.appointStaff(request, principal, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.created(appointed, "Hospital staff appointed and onboarded successfully"));
    }

    @GetMapping({"", "/", "/hospital-staff"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getHospitalStaff(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<UserResponse> staff = recruitmentService.getHospitalStaff(principal);
        return ResponseEntity.ok(ApiResponse.success(staff, "Hospital staff roster retrieved"));
    }
}
