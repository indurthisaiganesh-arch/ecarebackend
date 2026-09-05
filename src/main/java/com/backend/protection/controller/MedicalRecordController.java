package com.backend.protection.controller;

import com.backend.protection.dto.ApiResponse;
import com.backend.protection.dto.record.CreateRecordRequest;
import com.backend.protection.dto.record.MedicalRecordResponse;
import com.backend.protection.security.UserPrincipal;
import com.backend.protection.service.MedicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService recordService;

    public MedicalRecordController(MedicalRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getRecordsByPatient(
            @PathVariable String patientId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        List<MedicalRecordResponse> records = recordService.getRecordsForPatient(patientId, principal, request);
        return ResponseEntity.ok(ApiResponse.success(records, "Medical records retrieved and decrypted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> getRecordById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        MedicalRecordResponse record = recordService.getRecordById(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success(record, "Medical record retrieved and decrypted successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> createRecord(
            @Valid @RequestBody CreateRecordRequest createRequest,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        MedicalRecordResponse created = recordService.createRecord(createRequest, principal, request);
        return ResponseEntity.status(201).body(ApiResponse.created(created, "Medical record encrypted with AES-256-GCM and saved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {

        recordService.deleteRecord(id, principal, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Medical record deleted"));
    }
}
