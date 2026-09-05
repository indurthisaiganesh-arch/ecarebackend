package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.patient.CreatePatientRequest;
import com.backend.protection.dto.patient.PatientResponse;
import com.backend.protection.dto.patient.UpdatePatientRequest;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> getAllPatients(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<PatientResponse> patients = patientService.getAllPatients(principal, search, pageRequest, request);
        return ResponseEntity.ok(ApiResponse.success(patients, "Patients retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        PatientResponse patient = patientService.getPatientById(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success(patient, "Patient details retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody CreatePatientRequest createRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        PatientResponse created = patientService.createPatient(createRequest, principal, request);
        return ResponseEntity.status(201).body(ApiResponse.created(created, "Patient registered successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable String id,
            @Valid @RequestBody UpdatePatientRequest updateRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        PatientResponse updated = patientService.updatePatient(id, updateRequest, principal, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Patient updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        patientService.deletePatient(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Patient deleted successfully"));
    }
}
