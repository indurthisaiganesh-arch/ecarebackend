package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.appointment.AppointmentResponse;
import com.backend.protection.dto.appointment.CreateAppointmentRequest;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'DOCTOR', 'ADMIN', 'PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        AppointmentResponse response = appointmentService.createAppointment(request, principal, httpRequest);
        return ResponseEntity.status(201).body(ApiResponse.created(response, "Doctor appointment scheduled successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AppointmentResponse> list = appointmentService.getAppointments(principal);
        return ResponseEntity.ok(ApiResponse.success(list, "Appointments retrieved successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {

        AppointmentResponse updated = appointmentService.updateStatus(id, status, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(updated, "Appointment status updated"));
    }
}
